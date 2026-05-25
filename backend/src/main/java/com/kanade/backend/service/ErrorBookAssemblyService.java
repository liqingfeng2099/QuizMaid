package com.kanade.backend.service;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.assembly.AssemblyOrchestrator;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ErrorBookMapper;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.StrategyWeightMapper;
import com.kanade.backend.model.dto.AssemblyRequestDTO;
import com.kanade.backend.model.entity.*;
import com.kanade.backend.model.vo.AssemblyResultVO;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorBookAssemblyService {

    private final ErrorBookMapper errorBookMapper;
    private final QuestionService questionService;
    private final ExamPaperService examPaperService;
    private final ExamPaperMapper examPaperMapper;
    private final PaperquestionMapper paperquestionMapper;
    private final AssemblyOrchestrator assemblyOrchestrator;
    private final PaperStrategyService paperStrategyService;
    private final StrategyWeightMapper strategyWeightMapper;

    @Transactional
    public ExamPaperVO assembleFromErrors(String paperName, Integer targetQuestionCount,
                                           Integer difficultyAvg, Integer duration) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 获取用户错题，提取特征
        QueryWrapper ebQw = QueryWrapper.create().eq("userId", userId).eq("isArchived", 0);
        List<ErrorBook> errors = errorBookMapper.selectListByQuery(ebQw);
        if (errors.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可用的错题");

        List<Long> errorQids = errors.stream().map(ErrorBook::getQuestionId).toList();
        List<Question> errorQuestions = questionService.listByIds(errorQids);

        // 提取知识点
        Set<String> kpSet = new HashSet<>();
        for (Question q : errorQuestions) {
            if (q.getKnowledgePoints() != null)
                for (String kp : q.getKnowledgePoints().split(","))
                    kpSet.add(kp.trim());
        }

        // 2. 复用统一组卷策略体系：创建临时策略（错题知识点权重≥70%）
        int totalScore = targetQuestionCount != null ? targetQuestionCount * 10 : 150;
        int diff = difficultyAvg != null ? difficultyAvg : 2;

        PaperStrategy strategy = new PaperStrategy();
        strategy.setUserId(userId);
        strategy.setStrategyName("错题强化-" + System.currentTimeMillis());
        strategy.setTotalScore(totalScore);
        strategy.setDifficultyAvg(diff);
        strategy.setDuration(duration);
        strategy.setKnowledgePointScope(
                cn.hutool.json.JSONUtil.toJsonStr(kpSet.stream().collect(Collectors.toList())));
        strategy.setIsDefault(0);
        strategy.setCreateTime(java.time.LocalDateTime.now());
        strategy.setUpdateTime(java.time.LocalDateTime.now());
        paperStrategyService.save(strategy);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 创建权重：侧重知识点匹配 + 难度
        StrategyWeight wKp = new StrategyWeight(); wKp.setStrategyId(strategy.getId());
        wKp.setWeightType("knowledgeCount"); wKp.setWeightValue(40); wKp.setCreateTime(now); wKp.setUpdateTime(now);
        strategyWeightMapper.insert(wKp);
        StrategyWeight wDiff = new StrategyWeight(); wDiff.setStrategyId(strategy.getId());
        wDiff.setWeightType("difficulty"); wDiff.setWeightValue(30); wDiff.setCreateTime(now); wDiff.setUpdateTime(now);
        strategyWeightMapper.insert(wDiff);
        StrategyWeight wAcc = new StrategyWeight(); wAcc.setStrategyId(strategy.getId());
        wAcc.setWeightType("accuracy"); wAcc.setWeightValue(15); wAcc.setCreateTime(now); wAcc.setUpdateTime(now);
        strategyWeightMapper.insert(wAcc);
        StrategyWeight wDisc = new StrategyWeight(); wDisc.setStrategyId(strategy.getId());
        wDisc.setWeightType("discrimination"); wDisc.setWeightValue(5); wDisc.setCreateTime(now); wDisc.setUpdateTime(now);
        strategyWeightMapper.insert(wDisc);
        StrategyWeight wCalc = new StrategyWeight(); wCalc.setStrategyId(strategy.getId());
        wCalc.setWeightType("calcLevel"); wCalc.setWeightValue(5); wCalc.setCreateTime(now); wCalc.setUpdateTime(now);
        strategyWeightMapper.insert(wCalc);
        StrategyWeight wFreq = new StrategyWeight(); wFreq.setStrategyId(strategy.getId());
        wFreq.setWeightType("examFrequency"); wFreq.setWeightValue(5); wFreq.setCreateTime(now); wFreq.setUpdateTime(now);
        strategyWeightMapper.insert(wFreq);

        List<StrategyWeight> weights = strategyWeightMapper.selectListByQuery(
                QueryWrapper.create().eq("strategyId", strategy.getId()));

        // 3. 调用统一组卷算法（贪心快速组卷）
        String subject = errorQuestions.stream()
                .filter(q -> q.getSubject() != null).map(Question::getSubject)
                .findFirst().orElse("综合");

        AssemblyRequestDTO request = new AssemblyRequestDTO();
        request.setStrategyId(strategy.getId());
        request.setSubject(subject);
        request.setPaperName(paperName != null ? paperName : "错题强化卷");
        request.setPaperStatus(0);

        AssemblyResultVO result = assemblyOrchestrator.greedyAssemble(request, strategy, weights, userId);

        if (result.getTotalQuestions() == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "组卷失败：候选题目不足");
        }

        // 4. 保存试卷
        java.time.LocalDateTime now2 = java.time.LocalDateTime.now();
        ExamPaper paper = ExamPaper.builder()
                .paperName(paperName != null ? paperName : "错题强化卷")
                .subject(subject)
                .totalScore(0)
                .creatorId(userId)
                .status(0)
                .strategyId(strategy.getId())
                .paperType(2)
                .difficultyRate(java.math.BigDecimal.valueOf(diff))
                .duration(duration)
                .exportStatus(0)
                .createTime(now2)
                .updateTime(now2)
                .build();
        examPaperMapper.insert(paper);

        int sort = 1;
        int total = 0;
        if (result.getQuestions() != null) {
            for (AssemblyResultVO.QuestionScoreVO qs : result.getQuestions()) {
                Paperquestion pq = Paperquestion.builder()
                        .paperId(paper.getId()).questionId(qs.getQuestionId())
                        .questionScore(qs.getScore() != null ? qs.getScore() : 10)
                        .sort(sort++).isAutoAdd(1)
                        .createTime(java.time.LocalDateTime.now()).build();
                paperquestionMapper.insert(pq);
                total += (qs.getScore() != null ? qs.getScore() : 10);
            }
        }
        paper.setTotalScore(total);
        examPaperMapper.update(paper);

        return examPaperService.getExamPaperVOById(paper.getId());
    }
}
