package com.kanade.backend.assembly.model;

import cn.hutool.json.JSONUtil;
import com.kanade.backend.model.entity.PaperStrategy;
import com.kanade.backend.model.entity.StrategyWeight;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AssemblyContext {

    private PaperStrategy strategy;
    private List<StrategyWeight> weights;
    private List<QuestionScore> candidatePool;
    private AssemblyConstraint constraints;
    private Map<IndicatorEnum, Integer> weightMap;

    public AssemblyContext() {}

    public PaperStrategy getStrategy() { return strategy; }
    public void setStrategy(PaperStrategy s) { this.strategy = s; }

    public List<StrategyWeight> getWeights() { return weights; }
    public void setWeights(List<StrategyWeight> w) { this.weights = w; }

    public List<QuestionScore> getCandidatePool() { return candidatePool; }
    public void setCandidatePool(List<QuestionScore> pool) { this.candidatePool = pool; }

    public AssemblyConstraint getConstraints() { return constraints; }
    public void setConstraints(AssemblyConstraint c) { this.constraints = c; }

    public Map<IndicatorEnum, Integer> getWeightMap() { return weightMap; }
    public void setWeightMap(Map<IndicatorEnum, Integer> map) { this.weightMap = map; }

    public static AssemblyContext from(PaperStrategy strategy, List<StrategyWeight> weights, List<QuestionScore> candidates) {
        Map<IndicatorEnum, Integer> wMap = weights.stream()
                .collect(Collectors.toMap(
                        w -> IndicatorEnum.fromCode(w.getWeightType()),
                        StrategyWeight::getWeightValue
                ));

        AssemblyConstraint constraints = AssemblyConstraint.builder()
                .targetTotalScore(strategy.getTotalScore())
                .targetDifficultyAvg(strategy.getDifficultyAvg())
                .build();

        if (strategy.getQuestionTypeConfig() != null && !strategy.getQuestionTypeConfig().isBlank()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typeConfigs = (List) JSONUtil.toList(strategy.getQuestionTypeConfig(), Map.class);
            Map<Integer, AssemblyConstraint.TypeConstraint> typeMap = typeConfigs.stream()
                    .collect(Collectors.toMap(
                            m -> (Integer) m.get("type"),
                            m -> AssemblyConstraint.TypeConstraint.builder()
                                    .count((Integer) m.get("count"))
                                    .scorePerQuestion((Integer) m.get("score"))
                                    .build()
                    ));
            constraints.setTypeConstraints(typeMap);
        }

        if (strategy.getDifficultyConfig() != null && !strategy.getDifficultyConfig().isBlank()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diffConfigs = (List) JSONUtil.toList(strategy.getDifficultyConfig(), Map.class);
            Map<Integer, Double> diffMap = diffConfigs.stream()
                    .collect(Collectors.toMap(
                            m -> (Integer) m.get("level"),
                            m -> ((Number) m.get("ratio")).doubleValue()
                    ));
            constraints.setDifficultyRatios(diffMap);
        }

        AssemblyContext ctx = new AssemblyContext();
        ctx.strategy = strategy;
        ctx.weights = weights;
        ctx.candidatePool = candidates;
        ctx.constraints = constraints;
        ctx.weightMap = wMap;
        return ctx;
    }
}
