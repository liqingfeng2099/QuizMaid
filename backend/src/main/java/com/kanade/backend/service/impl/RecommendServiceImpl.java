package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.kanade.backend.mapper.ErrorBookMapper;
import com.kanade.backend.mapper.RecommendFeedbackMapper;
import com.kanade.backend.mapper.UseranswerdetailMapper;
import com.kanade.backend.mapper.UserexamrecordMapper;
import com.kanade.backend.model.dto.RecommendQueryDTO;
import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.entity.RecommendFeedback;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionService;
import com.kanade.backend.service.RecommendService;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    private final ErrorBookMapper errorBookMapper;
    private final QuestionService questionService;
    private final UseranswerdetailMapper useranswerdetailMapper;
    private final UserexamrecordMapper userexamrecordMapper;
    private final RecommendFeedbackMapper feedbackMapper;

    @Override
    public List<QuestionVO> recommendSimilarQuestions(RecommendQueryDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        int count = dto.getCount() != null ? Math.clamp(dto.getCount(), 5, 30) : 15;
        String tendency = dto.getDifficultyTendency() != null ? dto.getDifficultyTendency() : "balanced";

        // 1. 获取用户错题的知识点、题型、难度
        QueryWrapper ebQw = QueryWrapper.create().eq("userId", userId).eq("isArchived", 0);
        List<ErrorBook> errors = errorBookMapper.selectListByQuery(ebQw);
        if (errors.isEmpty()) return Collections.emptyList();

        List<Long> errorQids = errors.stream().map(ErrorBook::getQuestionId).toList();
        List<Question> errorQuestions = questionService.listByIds(errorQids);
        Map<Long, Question> eqMap = errorQuestions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 2. 提取错题知识点，同时加载历史反馈调整权重 (C2)
        Set<String> allKps = new HashSet<>();
        Set<Integer> allTypes = new HashSet<>();
        List<Integer> allDiffs = new ArrayList<>();
        for (ErrorBook eb : errors) {
            Question q = eqMap.get(eb.getQuestionId());
            if (q != null) {
                if (q.getKnowledgePoints() != null)
                    for (String kp : q.getKnowledgePoints().split(","))
                        allKps.add(kp.trim());
                if (q.getType() != null) allTypes.add(q.getType());
                if (q.getDifficulty() != null) allDiffs.add(q.getDifficulty());
            }
        }

        // 3. 加载历史反馈，计算每个知识点的掌握度权重 (C2)
        Map<String, Double> kpWeights = buildKpWeights(userId, allKps);

        // 4. 获取过滤集合
        Set<Long> recentCorrectQids = getRecentCorrectQuestionIds(userId);
        Set<Long> errorBookQids = new HashSet<>(errorQids);

        // 5. 按知识点匹配搜索（高权重知识点优先搜索更多候选）
        List<Question> candidates = new ArrayList<>();
        for (String kp : allKps) {
            double weight = kpWeights.getOrDefault(kp, 1.0);
            int candidateLimit = (int) Math.max(20, 100 * weight); // 高权重搜更多
            QueryWrapper qw = QueryWrapper.create()
                    .like("knowledgePoints", kp)
                    .eq("creatorId", userId)
                    .limit(candidateLimit);
            List<Question> found = questionService.list(qw);
            for (Question q : found) {
                if (!errorBookQids.contains(q.getId()) &&
                    !recentCorrectQids.contains(q.getId()) &&
                    allTypes.contains(q.getType())) {
                    candidates.add(q);
                }
            }
            if (candidates.size() >= count * 3) break;
        }

        // 6. 按难度梯度 + 知识点权重 综合排序
        candidates.sort((a, b) -> {
            int aMatch = matchDifficulty(a.getDifficulty(), allDiffs, tendency);
            int bMatch = matchDifficulty(b.getDifficulty(), allDiffs, tendency);
            // 知识点权重加成
            double aKpBonus = getKpBonus(a, allKps, kpWeights);
            double bKpBonus = getKpBonus(b, allKps, kpWeights);
            return Double.compare(bMatch + bKpBonus, aMatch + aKpBonus);
        });

        // 7. 去重取前N
        Set<Long> seen = new HashSet<>();
        List<Question> selected = new ArrayList<>();
        for (Question q : candidates) {
            if (seen.add(q.getId())) selected.add(q);
            if (selected.size() >= count) break;
        }

        return selected.stream().map(q -> {
            QuestionVO vo = new QuestionVO();
            BeanUtils.copyProperties(q, vo);
            vo.setAnswer(null);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void recordFeedback(Long questionId, boolean correct) {
        Long userId = StpUtil.getLoginIdAsLong();
        try {
            // 获取题目的知识点
            Question q = questionService.getById(questionId);
            String kps = q != null ? q.getKnowledgePoints() : "";

            RecommendFeedback fb = new RecommendFeedback();
            fb.setUserId(userId);
            fb.setQuestionId(questionId);
            fb.setKnowledgePoints(kps);
            fb.setFeedback(correct ? 1 : 2); // 1=已掌握 2=仍困难
            fb.setCreateTime(LocalDateTime.now());
            feedbackMapper.insert(fb);

            log.info("[推荐反馈] userId={} questionId={} feedback={} kps={}",
                    userId, questionId, fb.getFeedback(), kps);
        } catch (Exception e) {
            log.warn("[推荐反馈] 持久化失败: userId={} questionId={}", userId, questionId, e);
        }
    }

    /**
     * 根据用户历史反馈构建知识点权重 (C2)
     * 已掌握 → 权重降低 (0.3); 仍困难 → 权重提高 (1.5); 无反馈 → 权重默认 (1.0)
     */
    private Map<String, Double> buildKpWeights(Long userId, Set<String> allKps) {
        Map<String, Double> weights = new HashMap<>();
        // 默认权重
        for (String kp : allKps) weights.put(kp, 1.0);
        try {
            QueryWrapper qw = QueryWrapper.create()
                    .eq("userId", userId)
                    .ge("createTime", LocalDateTime.now().minusDays(30));
            List<RecommendFeedback> fbList = feedbackMapper.selectListByQuery(qw);

            for (RecommendFeedback fb : fbList) {
                if (fb.getKnowledgePoints() == null) continue;
                for (String kp : fb.getKnowledgePoints().split(",")) {
                    kp = kp.trim();
                    if (!weights.containsKey(kp)) continue;
                    double current = weights.get(kp);
                    if (fb.getFeedback() != null && fb.getFeedback() == 1) {
                        // 已掌握：降低该知识点权重
                        weights.put(kp, Math.max(0.3, current - 0.2));
                    } else if (fb.getFeedback() != null && fb.getFeedback() == 2) {
                        // 仍困难：提高该知识点权重
                        weights.put(kp, Math.min(2.0, current + 0.3));
                    }
                }
            }
            log.debug("[推荐权重] userId={} weights={}", userId, weights);
        } catch (Exception e) {
            log.warn("[推荐权重] 计算失败", e);
        }
        return weights;
    }

    private double getKpBonus(Question q, Set<String> allKps, Map<String, Double> kpWeights) {
        if (q == null || q.getKnowledgePoints() == null) return 0;
        double bonus = 0;
        for (String kp : q.getKnowledgePoints().split(",")) {
            kp = kp.trim();
            if (allKps.contains(kp)) {
                bonus += kpWeights.getOrDefault(kp, 1.0) - 1.0; // 偏离基准的加成
            }
        }
        return bonus;
    }

    private int matchDifficulty(Integer diff, List<Integer> errorDiffs, String tendency) {
        if (diff == null) return 0;
        double avg = errorDiffs.stream().mapToInt(Integer::intValue).average().orElse(2);
        if ("basic".equals(tendency)) return diff <= avg ? 3 : (diff <= avg + 1 ? 1 : 0);
        if ("advanced".equals(tendency)) return diff >= avg ? 3 : (diff >= avg - 1 ? 1 : 0);
        return Math.abs(diff - Math.round(avg)) <= 1 ? 3 : 1;
    }

    private Set<Long> getRecentCorrectQuestionIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        try {
            QueryWrapper uerQw = QueryWrapper.create()
                    .eq("userId", userId)
                    .ge("createTime", LocalDateTime.now().minusDays(30));
            List<Long> recordIds = userexamrecordMapper.selectListByQuery(uerQw)
                    .stream().map(r -> r.getId()).toList();
            if (recordIds.isEmpty()) return ids;

            QueryWrapper uadQw = QueryWrapper.create()
                    .in("recordId", recordIds)
                    .eq("correctStatus", 1);
            useranswerdetailMapper.selectListByQuery(uadQw)
                    .forEach(d -> ids.add(d.getQuestionId()));
        } catch (Exception e) { log.warn("[推荐] 过滤近期正确题目失败", e); }
        return ids;
    }
}
