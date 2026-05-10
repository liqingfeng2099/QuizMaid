package com.kanade.backend.assembly.scorer;

import com.kanade.backend.assembly.model.AssemblyConstraint;
import com.kanade.backend.assembly.model.AssemblyContext;
import com.kanade.backend.assembly.model.IndicatorEnum;
import com.kanade.backend.assembly.model.PaperCandidate;
import com.kanade.backend.assembly.model.QuestionScore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FitnessCalculator {

    private final AssemblyContext context;

    public FitnessCalculator(AssemblyContext context) {
        this.context = context;
    }

    /**
     * 计算试卷候选方案的适应度 (0-1)
     * 适应度 = 各维度满足度的加权平均
     */
    public double calculate(PaperCandidate candidate) {
        List<QuestionScore> questions = candidate.getQuestionScores();
        if (questions == null || questions.isEmpty()) {
            return 0;
        }

        int n = questions.size();
        candidate.setTotalScore(questions.stream().mapToInt(q -> 10).sum());
        candidate.setAvgDifficulty(questions.stream().mapToInt(QuestionScore::getDifficulty).average().orElse(0));
        candidate.setAvgCalcLevel(questions.stream().mapToInt(QuestionScore::getCalcLevel).average().orElse(0));
        candidate.setAvgDiscrimination(questions.stream().mapToInt(QuestionScore::getDiscrimination).average().orElse(0));

        double fitness = 0;
        int totalWeight = 0;

        // 总分满足度
        AssemblyConstraint constraints = context.getConstraints();
        if (constraints != null && constraints.getTargetTotalScore() != null) {
            double scoreRatio = Math.min(1.0, (double) candidate.getTotalScore() / constraints.getTargetTotalScore());
            fitness += scoreRatio * getWeight(IndicatorEnum.DIFFICULTY);
            totalWeight += getWeight(IndicatorEnum.DIFFICULTY); // use difficulty weight for total score
        }

        // 题型分布满足度
        if (constraints != null && constraints.getTypeConstraints() != null && !constraints.getTypeConstraints().isEmpty()) {
            Map<Integer, Long> actualTypeCount = questions.stream()
                    .collect(Collectors.groupingBy(QuestionScore::getType, Collectors.counting()));
            double typeMatch = 0;
            int typeCount = 0;
            for (Map.Entry<Integer, AssemblyConstraint.TypeConstraint> entry : constraints.getTypeConstraints().entrySet()) {
                long actual = actualTypeCount.getOrDefault(entry.getKey(), 0L);
                int expected = entry.getValue().getCount();
                typeMatch += expected > 0 ? Math.min(1.0, (double) actual / expected) : 1.0;
                typeCount++;
            }
            if (typeCount > 0) {
                typeMatch /= typeCount;
                fitness += typeMatch * getWeight(IndicatorEnum.KNOWLEDGE_COUNT);
                totalWeight += getWeight(IndicatorEnum.KNOWLEDGE_COUNT);
            }
        }

        // 难度分布满足度
        if (constraints != null && constraints.getDifficultyRatios() != null && !constraints.getDifficultyRatios().isEmpty()) {
            Map<Integer, Long> actualDiffCount = questions.stream()
                    .collect(Collectors.groupingBy(QuestionScore::getDifficulty, Collectors.counting()));
            double diffMatch = 0;
            int diffCount = 0;
            for (Map.Entry<Integer, Double> entry : constraints.getDifficultyRatios().entrySet()) {
                long actual = actualDiffCount.getOrDefault(entry.getKey(), 0L);
                double expectedRatio = entry.getValue();
                double actualRatio = (double) actual / n;
                double diff = Math.abs(actualRatio - expectedRatio);
                diffMatch += Math.max(0, 1.0 - diff / expectedRatio);
                diffCount++;
            }
            if (diffCount > 0) {
                diffMatch /= diffCount;
                fitness += diffMatch * getWeight(IndicatorEnum.DIFFICULTY);
                totalWeight += getWeight(IndicatorEnum.DIFFICULTY);
            }
        }

        // 各维度指标加权贡献
        for (Map.Entry<IndicatorEnum, Integer> entry : context.getWeightMap().entrySet()) {
            IndicatorEnum indicator = entry.getKey();
            int weight = entry.getValue();
            if (weight == 0 || indicator == null) continue;

            double avgNormalized = questions.stream()
                    .mapToDouble(q -> q.getNormalizedValues() != null
                            ? q.getNormalizedValues().getOrDefault(indicator, 0.5)
                            : 0.5)
                    .average()
                    .orElse(0.5);

            fitness += avgNormalized * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? Math.min(1.0, fitness / totalWeight) : 0.5;
    }

    private int getWeight(IndicatorEnum indicator) {
        return context.getWeightMap() != null ? context.getWeightMap().getOrDefault(indicator, 10) : 10;
    }
}
