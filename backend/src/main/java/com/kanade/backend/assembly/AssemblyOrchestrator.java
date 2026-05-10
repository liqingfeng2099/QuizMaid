package com.kanade.backend.assembly;

import cn.hutool.json.JSONUtil;
import com.kanade.backend.assembly.constraint.ConstraintValidator;
import com.kanade.backend.assembly.model.*;
import com.kanade.backend.assembly.scorer.CompositeScorer;
import com.kanade.backend.assembly.scorer.FitnessCalculator;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.QuestionMapper;
import com.kanade.backend.model.dto.AssemblyDegradeHintDTO;
import com.kanade.backend.model.dto.AssemblyRequestDTO;
import com.kanade.backend.model.entity.*;
import com.kanade.backend.model.vo.AssemblyResultVO;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssemblyOrchestrator {

    private final QuestionMapper questionMapper;
    private final PaperquestionMapper paperquestionMapper;

    /**
     * 手动组卷：贪心算法
     */
    public AssemblyResultVO greedyAssemble(AssemblyRequestDTO request, PaperStrategy strategy,
                                           List<StrategyWeight> weights, Long userId) {
        // 1. 查询候选题库
        List<Question> candidateQuestions = fetchCandidates(request, userId);

        // 2. 构建上下文
        AssemblyContext context = AssemblyContext.from(strategy, weights, Collections.emptyList());
        context.setCandidatePool(Collections.emptyList()); // will be filled
        CompositeScorer scorer = new CompositeScorer(context);

        // 3. 计算每题得分
        List<QuestionScore> scoredCandidates = candidateQuestions.stream()
                .map(scorer::scoreWithDetail)
                .filter(qs -> qs.getCompositeScore() > 0)
                .collect(Collectors.toList());

        log.info("[组卷] 候选题目: {}道, 有效题目(得分>0): {}道",
                candidateQuestions.size(), scoredCandidates.size());

        if (scoredCandidates.isEmpty()) {
            return AssemblyResultVO.builder()
                    .totalQuestions(0)
                    .actualTotalScore(0)
                    .fitness(0.0)
                    .algorithmType("GREEDY")
                    .questions(Collections.emptyList())
                    .dimensionResults(Map.of("error", "没有符合条件的题目，请调整筛选条件或扩充题库"))
                    .build();
        }

        // 4. 执行贪心算法
        GreedyAlgorithm greedy = new GreedyAlgorithm(scorer, context.getConstraints());
        GreedyAlgorithm.GreedyResult result = greedy.assemble(scoredCandidates);

        // 5. 构建返回结果
        return buildResult(result.selected(), result.degradeHints(), context, "GREEDY");
    }

    /**
     * 高精度组卷：遗传算法（用于AI组卷或正式组卷）
     */
    public AssemblyResultVO geneticAssemble(AssemblyRequestDTO request, PaperStrategy strategy,
                                            List<StrategyWeight> weights, Long userId) {
        List<Question> candidateQuestions = fetchCandidates(request, userId);

        AssemblyContext context = AssemblyContext.from(strategy, weights, Collections.emptyList());
        CompositeScorer scorer = new CompositeScorer(context);
        FitnessCalculator fitnessCalc = new FitnessCalculator(context);

        List<QuestionScore> scoredCandidates = candidateQuestions.stream()
                .map(scorer::scoreWithDetail)
                .filter(qs -> qs.getCompositeScore() > 0)
                .collect(Collectors.toList());

        if (scoredCandidates.isEmpty()) {
            return AssemblyResultVO.builder()
                    .totalQuestions(0)
                    .actualTotalScore(0)
                    .fitness(0.0)
                    .algorithmType("GENETIC")
                    .questions(Collections.emptyList())
                    .dimensionResults(Map.of("error", "没有符合条件的题目"))
                    .build();
        }

        // 执行遗传算法
        GeneticAlgorithm ga = new GeneticAlgorithm(scorer, fitnessCalc, context.getConstraints());
        GeneticAlgorithm.GeneticResult result = ga.assemble(scoredCandidates);

        // 降级校验
        ConstraintValidator validator = new ConstraintValidator(context.getConstraints());
        List<AssemblyDegradeHintDTO> degradeHints = validator.validateWithHints(
                result.bestCandidate().getQuestionScores());

        return buildResult(result.bestCandidate().getQuestionScores(), degradeHints, context, "GENETIC");
    }

    /**
     * 将组卷结果保存为试卷
     */
    public ExamPaper saveAsPaper(List<QuestionScore> selected, PaperStrategy strategy, String paperName, Long userId) {
        ExamPaper paper = ExamPaper.builder()
                .paperName(paperName != null ? paperName : strategy.getStrategyName() + "-组卷")
                .subject("综合")
                .totalScore(0)
                .creatorId(userId)
                .status(0) // 草稿
                .strategyId(strategy.getId())
                .paperType(2) // 自动组卷
                .difficultyRate(java.math.BigDecimal.valueOf(strategy.getDifficultyAvg() != null ? strategy.getDifficultyAvg() : 3))
                .duration(strategy.getDuration())
                .exportStatus(0)
                .build();

        // 保存试卷需要 ExamPaperService，这里返回 ExamPaper 由调用方保存
        return paper;
    }

    private List<Question> fetchCandidates(AssemblyRequestDTO request, Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("creatorId", userId);

        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            wrapper.eq("subject", request.getSubject());
        }
        if (request.getChapter() != null && !request.getChapter().isBlank()) {
            wrapper.like("chapter", request.getChapter());
        }
        wrapper.limit(500);
        return questionMapper.selectListByQuery(wrapper);
    }

    private AssemblyResultVO buildResult(List<QuestionScore> selected, List<AssemblyDegradeHintDTO> hints,
                                          AssemblyContext context, String algorithmType) {
        List<AssemblyResultVO.QuestionScoreVO> questionVOs = new ArrayList<>();
        int totalScore = 0;

        for (int i = 0; i < selected.size(); i++) {
            QuestionScore qs = selected.get(i);
            int score = 10; // default score per question
            totalScore += score;

            questionVOs.add(AssemblyResultVO.QuestionScoreVO.builder()
                    .questionId(qs.getQuestionId())
                    .type(qs.getType())
                    .content(truncate(qs.getQuestion().getContent(), 60))
                    .difficulty(qs.getDifficulty())
                    .score(score)
                    .compositeScore(Math.round(qs.getCompositeScore() * 10000.0) / 10000.0)
                    .build());
        }

        // 维度校验结果
        Map<String, String> dimensionResults = new LinkedHashMap<>();
        dimensionResults.put("totalScore", "实际总分: " + totalScore +
                (context.getStrategy().getTotalScore() != null ? " / 目标: " + context.getStrategy().getTotalScore() : ""));
        dimensionResults.put("questionCount", "总题数: " + selected.size());
        dimensionResults.put("algorithmType", algorithmType);

        double fitness = 0;
        if (!hints.isEmpty()) {
            dimensionResults.put("degradeNote", "存在" + hints.size() + "项降级，详见降级提示");
        }

        return AssemblyResultVO.builder()
                .questions(questionVOs)
                .totalQuestions(selected.size())
                .actualTotalScore(totalScore)
                .dimensionResults(dimensionResults)
                .fitness(fitness)
                .algorithmType(algorithmType)
                .degradeHints(hints.isEmpty() ? null : hints)
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
