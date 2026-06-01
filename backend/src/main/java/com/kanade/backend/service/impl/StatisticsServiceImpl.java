package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.StatisticsMapper;
import com.kanade.backend.mapper.UseranswerdetailMapper;
import com.kanade.backend.mapper.UserexamrecordMapper;
import com.kanade.backend.model.dto.StatisticsQueryDTO;
import com.kanade.backend.model.dto.TrendQueryDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.entity.Useranswerdetail;
import com.kanade.backend.model.entity.Userexamrecord;
import com.kanade.backend.model.vo.*;
import com.kanade.backend.service.QuestionService;
import com.kanade.backend.service.StatisticsService;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;
    private final ExamPaperMapper examPaperMapper;
    private final UserexamrecordMapper userexamrecordMapper;
    private final UseranswerdetailMapper useranswerdetailMapper;
    private final QuestionService questionService;

    @Override
    public PaperStatisticsVO getPaperStatistics(StatisticsQueryDTO queryDTO) {
        Long paperId = queryDTO.getPaperId();
        if (paperId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }

        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }

        // 权限校验：普通用户只能查看自己创建的试卷统计
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin");
        if (!isAdmin && !paper.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权查看他人试卷统计");
        }

        PaperStatisticsVO vo = new PaperStatisticsVO();
        vo.setPaperId(paperId);
        vo.setPaperName(paper.getPaperName());
        vo.setSubject(paper.getSubject());
        vo.setTotalScore(paper.getTotalScore());

        // 1. 基础聚合
        PaperScoreAggregateVO aggregate = statisticsMapper.selectPaperScoreAggregate(paperId);
        if (aggregate != null && aggregate.getTotalRecords() != null && aggregate.getTotalRecords() > 0) {
            vo.setMaxScore(aggregate.getMaxScore());
            vo.setMinScore(aggregate.getMinScore());
            vo.setAvgScore(aggregate.getAvgScore() != null ?
                    aggregate.getAvgScore().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            vo.setTotalExaminees(aggregate.getTotalRecords().intValue());

            // 中位数
            List<Integer> scores = statisticsMapper.selectScoresByPaperId(paperId);
            vo.setMedianScore(computeMedian(scores));

            // 高分率(>=90%)和及格率(>=60%)
            if (paper.getTotalScore() != null && paper.getTotalScore() > 0) {
                int highScoreThreshold = (int) (paper.getTotalScore() * 0.9);
                int passThreshold = (int) (paper.getTotalScore() * 0.6);
                long highCount = scores.stream().filter(s -> s >= highScoreThreshold).count();
                long passCount = scores.stream().filter(s -> s >= passThreshold).count();
                vo.setHighScoreRate(BigDecimal.valueOf(highCount * 100.0 / scores.size())
                        .setScale(2, RoundingMode.HALF_UP));
                vo.setPassRate(BigDecimal.valueOf(passCount * 100.0 / scores.size())
                        .setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            vo.setTotalExaminees(0);
            vo.setQuestionTypeStats(Collections.emptyList());
            vo.setDifficultyStats(Collections.emptyList());
            vo.setKnowledgePointStats(Collections.emptyList());
            vo.setHighFreqWrongQuestions(Collections.emptyList());
            vo.setScoreDistribution(Collections.emptyList());
            vo.setCalculationTimestamp(LocalDateTime.now());
            return vo;
        }

        // 2. 题型维度统计
        List<QuestionTypeStatVO> typeStats = statisticsMapper.selectByQuestionType(paperId);
        for (QuestionTypeStatVO ts : typeStats) {
            ts.setQuestionTypeName(getQuestionTypeName(ts.getQuestionType()));
            ts.setCorrectRate(calcRate(ts.getCorrectCount(), ts.getTotalCount()));
            ts.setScoreRate(calcRate(ts.getTotalActualScore() != null ? ts.getTotalActualScore().longValue() : 0,
                    ts.getTotalQuestionScore() != null ? ts.getTotalQuestionScore().longValue() : 0));
        }
        vo.setQuestionTypeStats(typeStats);

        // 3. 难度维度统计
        List<DifficultyStatVO> diffStats = statisticsMapper.selectByDifficulty(paperId);
        for (DifficultyStatVO ds : diffStats) {
            ds.setDifficultyName(getDifficultyName(ds.getDifficulty()));
            ds.setCorrectRate(calcRate(ds.getCorrectCount(), ds.getTotalCount()));
            ds.setScoreRate(calcRate(ds.getTotalActualScore() != null ? ds.getTotalActualScore().longValue() : 0,
                    ds.getTotalQuestionScore() != null ? ds.getTotalQuestionScore().longValue() : 0));
        }
        vo.setDifficultyStats(diffStats);

        // 4. 知识点维度统计（拆分逗号分隔的知识点字符串）
        vo.setKnowledgePointStats(computeKnowledgePointStats(paperId));

        // 5. 分数段分布
        int bucketSize = paper.getTotalScore() != null && paper.getTotalScore() > 0 ?
                Math.max(paper.getTotalScore() / 10, 1) : 10;
        List<ScoreDistributionVO> dist = statisticsMapper.selectScoreDistribution(paperId, bucketSize);
        vo.setScoreDistribution(dist);

        // 6. 高频错题
        List<HighFreqWrongQuestionVO> wrongQuestions = statisticsMapper.selectHighFreqWrongQuestions(paperId, 20);
        for (HighFreqWrongQuestionVO wq : wrongQuestions) {
            wq.setQuestionTypeName(getQuestionTypeName(wq.getQuestionType()));
            wq.setDifficultyName(getDifficultyName(wq.getDifficulty()));
            // 截断题干内容用于列表展示
            if (wq.getQuestionContent() != null && wq.getQuestionContent().length() > 100) {
                wq.setQuestionContent(wq.getQuestionContent().substring(0, 100) + "...");
            }
        }
        vo.setHighFreqWrongQuestions(wrongQuestions);

        vo.setCalculationTimestamp(LocalDateTime.now());
        return vo;
    }

    @Override
    public List<TrendDataPointVO> getScoreTrend(TrendQueryDTO queryDTO) {
        Long userId = queryDTO.getUserId();
        if (userId == null) {
            userId = StpUtil.getLoginIdAsLong();
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin");
        if (!isAdmin && !userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权查看他人成绩趋势");
        }

        int limit = queryDTO.getLimit() != null ? queryDTO.getLimit() : 50;
        List<TrendDataPointVO> list = statisticsMapper.selectUserScoreTrend(
                userId, queryDTO.getSubject(), limit);

        // 计算得分率
        for (TrendDataPointVO point : list) {
            // 通过examPaper获取totalScore来计算scoreRate
            // TrendDataPointVO.totalScore is set via SQL alias to ep.total_score
        }

        return list;
    }

    @Override
    public PaperStatisticsVO getComparison(Long paperId) {
        if (paperId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 查询当前用户在该试卷的考试记录
        QueryWrapper userWrapper = QueryWrapper.create()
                .eq("paperId", paperId)
                .eq("userId", currentUserId)
                .in("status", Arrays.asList(1, 2));
        List<Userexamrecord> userRecords = userexamrecordMapper.selectListByQuery(userWrapper);

        // 查询所有考试记录
        QueryWrapper allWrapper = QueryWrapper.create()
                .eq("paperId", paperId)
                .in("status", Arrays.asList(1, 2));
        List<Userexamrecord> allRecords = userexamrecordMapper.selectListByQuery(allWrapper);

        if (allRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该试卷暂无考试记录");
        }

        PaperStatisticsVO vo = new PaperStatisticsVO();
        vo.setPaperId(paperId);
        vo.setTotalExaminees(allRecords.size());

        // 计算整体统计
        List<Integer> allScores = allRecords.stream()
                .map(r -> r.getUserScore() != null ? r.getUserScore() : 0)
                .sorted().collect(Collectors.toList());
        vo.setAvgScore(BigDecimal.valueOf(allScores.stream().mapToInt(Integer::intValue).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP));
        vo.setMaxScore(BigDecimal.valueOf(allScores.get(allScores.size() - 1)));
        vo.setMinScore(BigDecimal.valueOf(allScores.get(0)));
        vo.setMedianScore(computeMedian(allScores));

        return vo;
    }

    @Override
    public List<PaperStatisticsVO> getAvailablePapers() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin");

        List<PaperStatisticsVO> papers = statisticsMapper.selectAvailablePapers();
        if (!isAdmin) {
            // 过滤：仅返回当前用户创建的试卷
            papers = papers.stream()
                    .filter(p -> p.getCreatorId() != null && p.getCreatorId().equals(currentUserId))
                    .collect(Collectors.toList());
        }
        return papers;
    }

    // 获取全部题型正确率统计，调用mapper查询并设置名称、正确率、得分率 .hml
    @Override
    public List<QuestionTypeStatVO> getTypeAccuracy() {
        List<QuestionTypeStatVO> list = statisticsMapper.selectTypeAccuracy();
        for (QuestionTypeStatVO vo : list) {
            vo.setQuestionTypeName(getQuestionTypeName(vo.getQuestionType()));
            vo.setCorrectRate(calcRate(vo.getCorrectCount(), vo.getTotalCount()));
            vo.setScoreRate(calcRate(
                    vo.getTotalActualScore() != null ? vo.getTotalActualScore().longValue() : 0,
                    vo.getTotalQuestionScore() != null ? vo.getTotalQuestionScore().longValue() : 0));
        }
        return list;
    }

    // 获取指定试卷的题型正确率统计，校验参数后调用mapper并设置名称、正确率、得分率 .hml
    @Override
    public List<QuestionTypeStatVO> getPaperTypeAccuracy(Long paperId) {
        if (paperId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        List<QuestionTypeStatVO> list = statisticsMapper.selectPaperTypeAccuracy(paperId);
        for (QuestionTypeStatVO vo : list) {
            vo.setQuestionTypeName(getQuestionTypeName(vo.getQuestionType()));
            vo.setCorrectRate(calcRate(vo.getCorrectCount(), vo.getTotalCount()));
            vo.setScoreRate(calcRate(
                    vo.getTotalActualScore() != null ? vo.getTotalActualScore().longValue() : 0,
                    vo.getTotalQuestionScore() != null ? vo.getTotalQuestionScore().longValue() : 0));
        }
        return list;
    }

    // ========== 辅助方法 ==========

    private BigDecimal computeMedian(List<Integer> sortedScores) {
        if (sortedScores.isEmpty()) return BigDecimal.ZERO;
        int size = sortedScores.size();
        if (size % 2 == 1) {
            return BigDecimal.valueOf(sortedScores.get(size / 2));
        } else {
            double median = (sortedScores.get(size / 2 - 1) + sortedScores.get(size / 2)) / 2.0;
            return BigDecimal.valueOf(median).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calcRate(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String getQuestionTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "填空题";
            case 4: return "简答题";
            default: return "未知";
        }
    }

    private String getDifficultyName(Integer difficulty) {
        if (difficulty == null) return "未知";
        switch (difficulty) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }

    /**
     * 知识点维度统计：拆分逗号分隔的知识点字符串后聚合
     */
    private List<KnowledgePointStatVO> computeKnowledgePointStats(Long paperId) {
        // 查询所有答题详情（含关联的question.knowledgePoints）
        List<KnowledgePointStatVO> rawStats = statisticsMapper.selectByKnowledgePointRaw(paperId);
        if (rawStats.isEmpty()) return Collections.emptyList();

        // 按单个知识点拆分并聚合
        Map<String, KnowledgePointStatVO> aggMap = new LinkedHashMap<>();
        for (KnowledgePointStatVO raw : rawStats) {
            if (StrUtil.isBlank(raw.getKnowledgePoint())) continue;
            String[] kps = raw.getKnowledgePoint().split(",");
            // 拆分后的每个知识点分享原始统计的份额
            int kpCount = kps.length;
            for (String kp : kps) {
                String key = kp.trim();
                if (key.isEmpty()) continue;
                aggMap.compute(key, (k, v) -> {
                    if (v == null) {
                        v = new KnowledgePointStatVO();
                        v.setKnowledgePoint(key);
                        v.setTotalCount(0L);
                        v.setCorrectCount(0L);
                        v.setTotalActualScore(0);
                        v.setTotalQuestionScore(0);
                    }
                    v.setTotalCount(v.getTotalCount() + raw.getTotalCount() / kpCount);
                    v.setCorrectCount(v.getCorrectCount() + raw.getCorrectCount() / kpCount);
                    v.setTotalActualScore(v.getTotalActualScore() +
                            (raw.getTotalActualScore() != null ? raw.getTotalActualScore() / kpCount : 0));
                    v.setTotalQuestionScore(v.getTotalQuestionScore() +
                            (raw.getTotalQuestionScore() != null ? raw.getTotalQuestionScore() / kpCount : 0));
                    return v;
                });
            }
        }

        List<KnowledgePointStatVO> result = new ArrayList<>(aggMap.values());
        for (KnowledgePointStatVO kps : result) {
            kps.setCorrectRate(calcRate(kps.getCorrectCount(), kps.getTotalCount()));
            kps.setScoreRate(calcRate(
                    kps.getTotalActualScore() != null ? kps.getTotalActualScore().longValue() : 0,
                    kps.getTotalQuestionScore() != null ? kps.getTotalQuestionScore().longValue() : 0));
        }
        result.sort((a, b) -> Long.compare(
                b.getTotalCount() != null ? b.getTotalCount() : 0,
                a.getTotalCount() != null ? a.getTotalCount() : 0));
        return result;
    }
}
