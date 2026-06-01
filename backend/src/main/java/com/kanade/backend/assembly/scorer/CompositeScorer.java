package com.kanade.backend.assembly.scorer;

import com.kanade.backend.assembly.model.AssemblyContext;
import com.kanade.backend.assembly.model.IndicatorEnum;
import com.kanade.backend.assembly.model.QuestionScore;
import com.kanade.backend.model.entity.Question;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CompositeScorer {

    private final AssemblyContext context;

    public CompositeScorer(AssemblyContext context) {
        this.context = context;
    }

    /**
     * 计算单道题的综合加权得分
     * score(q) = Σ weight_i × normalize(q.indicator_i) / 100
     */
    public double score(Question q) {
        if (context.getWeightMap() == null || context.getWeightMap().isEmpty()) {
            return 0;
        }

        double total = 0;
        for (Map.Entry<IndicatorEnum, Integer> entry : context.getWeightMap().entrySet()) {
            IndicatorEnum indicator = entry.getKey();
            int weight = entry.getValue();
            if (weight == 0 || indicator == null) continue;

            double rawValue = extractRawValue(q, indicator);
            double normalized = normalizeForIndicator(indicator, rawValue);
            total += weight * normalized;
        }
        return total / 100.0;
    }

    /**
     * 批量评分，返回带得分和归一化值的 QuestionScore 列表
     */
    public QuestionScore scoreWithDetail(Question q) {
        QuestionScore qs = new QuestionScore();
        qs.setQuestion(q);

        Map<IndicatorEnum, Double> normValues = new HashMap<>();
        double total = 0;

        for (Map.Entry<IndicatorEnum, Integer> entry : context.getWeightMap().entrySet()) {
            IndicatorEnum indicator = entry.getKey();
            int weight = entry.getValue();
            if (indicator == null) continue;

            double rawValue = extractRawValue(q, indicator);
            double normalized = normalizeForIndicator(indicator, rawValue);
            normValues.put(indicator, normalized);

            if (weight > 0) {
                total += weight * normalized;
            }
        }

        // 知识点范围匹配加分：当策略指定了目标知识点时，匹配越多得分越高
        double kpBonus = calculateKpOverlapBonus(q);
        double compositeScore = total / 100.0 + kpBonus;

        qs.setNormalizedValues(normValues);
        qs.setCompositeScore(Math.min(1.0, compositeScore));
        return qs;
    }

    /**
     * 计算题目知识点与策略目标知识点的重叠加分
     * 匹配 1 个 +0.03，2 个 +0.06，3+ 个 +0.10（封顶）
     */
    private double calculateKpOverlapBonus(Question q) {
        List<String> targetKps = context.getKnowledgePointScope();
        if (targetKps == null || targetKps.isEmpty()) return 0;

        String qKps = q.getKnowledgePoints();
        if (qKps == null || qKps.isBlank()) return 0;

        int matchCount = 0;
        for (String qKp : qKps.split(",")) {
            String trimmed = qKp.trim();
            for (String target : targetKps) {
                if (trimmed.contains(target) || target.contains(trimmed)) {
                    matchCount++;
                    break;
                }
            }
        }

        if (matchCount == 0) return 0;
        if (matchCount == 1) return 0.03;
        if (matchCount == 2) return 0.06;
        return 0.10; // 3+ matches
    }

    private double extractRawValue(Question q, IndicatorEnum indicator) {
        if (indicator == IndicatorEnum.DIFFICULTY) {
            return q.getDifficulty() != null ? q.getDifficulty() : 3;
        } else if (indicator == IndicatorEnum.ACCURACY) {
            return q.getAccuracy() != null ? q.getAccuracy().doubleValue() * 100 : 50;
        } else if (indicator == IndicatorEnum.DISCRIMINATION) {
            return q.getDiscrimination() != null ? q.getDiscrimination() : 3;
        } else if (indicator == IndicatorEnum.CALC_LEVEL) {
            return q.getCalcLevel() != null ? q.getCalcLevel() : 2;
        } else if (indicator == IndicatorEnum.EXAM_FREQUENCY) {
            return q.getExamFrequency() != null ? q.getExamFrequency() : 50;
        } else if (indicator == IndicatorEnum.KNOWLEDGE_COUNT) {
            return q.getKnowledgePoints() != null
                    ? q.getKnowledgePoints().split(",").length
                    : 2;
        }
        return 0;
    }

    private double normalizeForIndicator(IndicatorEnum indicator, double rawValue) {
        if (indicator == IndicatorEnum.DIFFICULTY) {
            int target = context.getStrategy().getDifficultyAvg() != null
                    ? context.getStrategy().getDifficultyAvg() : 3;
            double diff = Math.abs(rawValue - target);
            return Math.max(0, 1.0 - diff / 4.0);
        } else if (indicator == IndicatorEnum.ACCURACY) {
            if (rawValue >= 60 && rawValue <= 80) return 1.0;
            if (rawValue > 80) return Math.max(0, 1.0 - (rawValue - 80) / 20.0);
            return Math.max(0, rawValue / 60.0);
        } else if (indicator == IndicatorEnum.DISCRIMINATION) {
            return (rawValue + 1) / 2.0;
        } else if (indicator == IndicatorEnum.CALC_LEVEL) {
            double diff = Math.abs(rawValue - 2);
            return Math.max(0, 1.0 - diff / 2.0);
        } else if (indicator == IndicatorEnum.EXAM_FREQUENCY) {
            if (rawValue >= 10 && rawValue <= 30) return 1.0;
            if (rawValue > 30) return Math.max(0, 1.0 - (rawValue - 30) / 70.0);
            return Math.max(0, rawValue / 10.0);
        } else if (indicator == IndicatorEnum.KNOWLEDGE_COUNT) {
            if (rawValue >= 2 && rawValue <= 3) return 1.0;
            if (rawValue > 3) return Math.max(0, 1.0 - (rawValue - 3) / 7.0);
            return rawValue / 2.0;
        }
        return 0.5;
    }
}
