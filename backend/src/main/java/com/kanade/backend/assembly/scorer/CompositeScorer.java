package com.kanade.backend.assembly.scorer;

import com.kanade.backend.assembly.model.AssemblyContext;
import com.kanade.backend.assembly.model.IndicatorEnum;
import com.kanade.backend.assembly.model.QuestionScore;
import com.kanade.backend.model.entity.Question;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
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

        qs.setNormalizedValues(normValues);
        qs.setCompositeScore(total / 100.0);
        return qs;
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
