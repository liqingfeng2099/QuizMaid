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

        log.info("[错题推荐] 开始, userId={}, count={}, tendency={}", userId, count, tendency);

        // 1. 获取用户错题
        QueryWrapper ebQw = QueryWrapper.create().eq("userId", userId).eq("isArchived", 0);
        List<ErrorBook> errors = errorBookMapper.selectListByQuery(ebQw);
        log.info("[错题推荐] ① 未归档错题数: {}", errors.size());

        if (errors.isEmpty()) {
            // 检查是否有已归档的错题
            long archivedCount = errorBookMapper.selectCountByQuery(
                    QueryWrapper.create().eq("userId", userId).eq("isArchived", 1));
            log.warn("[错题推荐] 无未归档错题(已归档:{}道), 返回空", archivedCount);
            return Collections.emptyList();
        }

        List<Long> errorQids = errors.stream().map(ErrorBook::getQuestionId).toList();
        List<Question> errorQuestions = questionService.listByIds(errorQids);
        log.info("[错题推荐] ② 错题关联题目数: {}(错题)/{}(有效)", errorQids.size(), errorQuestions.size());

        if (errorQuestions.isEmpty()) {
            log.error("[错题推荐] 所有错题关联的原始题目均已被删除, errorQids={}", errorQids);
            return Collections.emptyList();
        }

        Map<Long, Question> eqMap = errorQuestions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 2. 提取错题知识点、题型、难度
        Set<String> allKps = new HashSet<>();
        Set<Integer> allTypes = new HashSet<>();
        List<Integer> allDiffs = new ArrayList<>();
        int kpMissingCount = 0;
        for (ErrorBook eb : errors) {
            Question q = eqMap.get(eb.getQuestionId());
            if (q != null) {
                if (q.getKnowledgePoints() != null && !q.getKnowledgePoints().isBlank()) {
                    for (String kp : q.getKnowledgePoints().split(","))
                        allKps.add(kp.trim());
                } else {
                    kpMissingCount++;
                }
                if (q.getType() != null) allTypes.add(q.getType());
                if (q.getDifficulty() != null) allDiffs.add(q.getDifficulty());
            }
        }
        log.info("[错题推荐] ③ 提取: KPs={}, types={}, diffs={}, 缺知识点题数={}",
                allKps, allTypes, allDiffs, kpMissingCount);

        if (allKps.isEmpty()) {
            log.error("[错题推荐] 所有错题均无知识点标签, 返回空");
            return Collections.emptyList();
        }
        if (allTypes.isEmpty()) {
            log.error("[错题推荐] 无法确定错题题型, 返回空");
            return Collections.emptyList();
        }

        // 3. 加载历史反馈权重
        Map<String, Double> kpWeights = buildKpWeights(userId, allKps);

        // 4. 获取过滤集合
        Set<Long> recentCorrectQids;
        if (Boolean.TRUE.equals(dto.getFilterRecentEnabled())) {
            int days = dto.getFilterRecentDays() != null ? dto.getFilterRecentDays() : 30;
            recentCorrectQids = getRecentCorrectQuestionIds(userId, days);
        } else {
            recentCorrectQids = Collections.emptySet();
        }
        Set<Long> errorBookQids = new HashSet<>(errorQids);
        log.info("[错题推荐] ④ 过滤集: 错题本{}道, 近期已做对{}(过滤{}), 天数配置={}",
                errorBookQids.size(), recentCorrectQids.size(),
                Boolean.TRUE.equals(dto.getFilterRecentEnabled()) ? "启用" : "关闭",
                dto.getFilterRecentDays() != null ? dto.getFilterRecentDays() : 30);

        // 5. 按知识点匹配搜索（含三级降级放宽，逐级累加）
        Set<Long> candidateIds = new HashSet<>();
        List<Question> candidates = new ArrayList<>();
        int degradeLevel = 0;

        // Level 0: 严格过滤
        addCandidates(candidates, candidateIds,
                searchCandidates(allKps, kpWeights, allTypes, userId,
                        errorBookQids, recentCorrectQids, count, 0));

        // 降级1: 取消"近30天已做对"过滤
        if (candidates.size() < count) {
            log.info("[错题推荐] 严格过滤仅{}道候选(需≥{}), 降级: 取消'近30天已做对'过滤",
                    candidates.size(), count);
            addCandidates(candidates, candidateIds,
                    searchCandidates(allKps, kpWeights, allTypes, userId,
                            errorBookQids, Collections.emptySet(), count, candidates.size()));
            degradeLevel = 1;
        }
        // 降级2: 取消题型限制
        if (candidates.size() < count) {
            log.info("[错题推荐] 降级1后仅{}道候选, 继续降级: 取消题型限制", candidates.size());
            addCandidates(candidates, candidateIds,
                    searchCandidates(allKps, kpWeights, Collections.emptySet(), userId,
                            errorBookQids, Collections.emptySet(), count, candidates.size()));
            degradeLevel = 2;
        }
        // 降级3: 取消创建者限制（搜全库）
        if (candidates.size() < count) {
            log.info("[错题推荐] 降级2后仅{}道候选, 继续降级: 取消创建者限制(搜全库)", candidates.size());
            addCandidates(candidates, candidateIds,
                    searchCandidates(allKps, kpWeights, allTypes, null,
                            errorBookQids, Collections.emptySet(), count, candidates.size()));
            degradeLevel = 3;
        }

        log.info("[错题推荐] ⑥ 候选总数: {}道 (降级级别: {})", candidates.size(), degradeLevel);

        // 降级4: 无新题可推 → 用错题本身做"重练推荐"
        if (candidates.isEmpty()) {
            log.warn("[错题推荐] 三级降级后仍无候选, 降级4: 使用错题本身作为重练推荐");
            for (Question q : errorQuestions) {
                if (candidateIds.add(q.getId())) {
                    candidates.add(q);
                }
            }
            degradeLevel = 4;
        }

        // 6. 按难度梯度 + 知识点权重 综合排序
        candidates.sort((a, b) -> {
            int aMatch = matchDifficulty(a.getDifficulty(), allDiffs, tendency);
            int bMatch = matchDifficulty(b.getDifficulty(), allDiffs, tendency);
            double aKpBonus = getKpBonus(a, allKps, kpWeights);
            double bKpBonus = getKpBonus(b, allKps, kpWeights);
            return Double.compare(bMatch + bKpBonus, aMatch + aKpBonus);
        });

        // 7. 去重取前N（candidates 已在 accumulate 阶段去重，此处直接截断）
        List<Question> selected = candidates.size() <= count
                ? candidates
                : candidates.subList(0, count);

        log.info("[错题推荐] ⑦ 最终推荐: {}道", selected.size());

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

    /**
     * 按知识点搜索候选题目。
     * @param creatorId 创建者限制，传 null 表示不限制（搜全库）
     * @param skipQids 已跳过的题目ID集合（不再重复加入）
     * @return 累积后的候选列表（会复用传入的 skip 语义做去重）
     */
    private List<Question> searchCandidates(Set<String> allKps, Map<String, Double> kpWeights,
                                             Set<Integer> allowedTypes, Long creatorId,
                                             Set<Long> excludeQids, Set<Long> recentCorrectQids,
                                             int targetCount, int alreadyHave) {
        List<Question> candidates = new ArrayList<>();
        for (String kp : allKps) {
            double weight = kpWeights.getOrDefault(kp, 1.0);
            int candidateLimit = (int) Math.max(20, 100 * weight);
            QueryWrapper qw = QueryWrapper.create()
                    .like("knowledgePoints", kp)
                    .limit(candidateLimit);
            if (creatorId != null) {
                qw.eq("creatorId", creatorId);
            }
            List<Question> found = questionService.list(qw);
            long added = 0;
            for (Question q : found) {
                if (excludeQids.contains(q.getId())) continue;
                if (!recentCorrectQids.isEmpty() && recentCorrectQids.contains(q.getId())) continue;
                if (!allowedTypes.isEmpty() && !allowedTypes.contains(q.getType())) continue;
                candidates.add(q);
                added++;
            }
            log.debug("[错题推荐] ⑤ KP='{}'(权重{:.2f}): 找到{}道, 加入{}道",
                    kp, weight, found.size(), added);
            if (candidates.size() + alreadyHave >= targetCount * 3) break;
        }
        return candidates;
    }

    /** 去重追加候选题目 */
    private void addCandidates(List<Question> target, Set<Long> seenIds, List<Question> source) {
        for (Question q : source) {
            if (seenIds.add(q.getId())) {
                target.add(q);
            }
        }
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

    private Set<Long> getRecentCorrectQuestionIds(Long userId, int days) {
        Set<Long> ids = new HashSet<>();
        try {
            QueryWrapper uerQw = QueryWrapper.create()
                    .eq("userId", userId)
                    .ge("createTime", LocalDateTime.now().minusDays(days));
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
