package com.kanade.backend.assembly;

import com.kanade.backend.assembly.constraint.ConstraintValidator;
import com.kanade.backend.assembly.model.AssemblyConstraint;
import com.kanade.backend.assembly.model.IndicatorEnum;
import com.kanade.backend.assembly.model.QuestionScore;
import com.kanade.backend.assembly.scorer.CompositeScorer;
import com.kanade.backend.model.dto.AssemblyDegradeHintDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GreedyAlgorithm {

    private final CompositeScorer scorer;
    private final AssemblyConstraint constraints;

    public GreedyAlgorithm(CompositeScorer scorer, AssemblyConstraint constraints) {
        this.scorer = scorer;
        this.constraints = constraints;
    }

    /**
     * 贪心组卷
     * 1. 为所有候选题目计算加权分并排序
     * 2. 单次遍历，按优先级依次尝试加入
     * 3. 加入后不违反硬约束 → 选中
     * 4. 题目不足时自动降级
     */
    public GreedyResult assemble(List<QuestionScore> candidatePool) {
        long startTime = System.currentTimeMillis();

        // 1. 排序（优先按题型分组，同题型内按加权分降序）
        List<QuestionScore> sorted = candidatePool.stream()
                .sorted(Comparator.comparingInt(QuestionScore::getType)
                        .thenComparing(Comparator.comparingDouble(QuestionScore::getCompositeScore).reversed()))
                .collect(Collectors.toList());

        // 2. 贪心选择
        List<QuestionScore> selected = new ArrayList<>();
        ConstraintValidator validator = new ConstraintValidator(constraints, 0);

        for (QuestionScore qs : sorted) {
            if (validator.canAdd(selected, qs)) {
                selected.add(qs);

                // 快速剪枝：如果所有题型都满足数量要求，可以提前停止
                if (allTypesSatisfied(selected)) {
                    log.debug("[贪心] 所有题型已满足，提前停止遍历");
                    break;
                }
            }
        }

        // 3. 校验 + 降级
        List<AssemblyDegradeHintDTO> degradeHints = validator.validateWithHints(selected);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[贪心] 组卷完成，耗时: {}ms, 选中题目: {}道, 降级提示: {}条",
                elapsed, selected.size(), degradeHints.size());

        return new GreedyResult(selected, degradeHints, elapsed);
    }

    private boolean allTypesSatisfied(List<QuestionScore> selected) {
        if (constraints == null || constraints.getTypeConstraints() == null || constraints.getTypeConstraints().isEmpty()) {
            return false;
        }
        return constraints.getTypeConstraints().entrySet().stream()
                .allMatch(entry -> selected.stream()
                        .filter(q -> q.getType().equals(entry.getKey()))
                        .count() >= entry.getValue().getCount());
    }

    /**
     * 贪心初始化种群（为遗传算法提供初始种群）
     * 用不同的排序优先级生成多个不同的试卷
     */
    public List<List<QuestionScore>> generateInitialPopulation(List<QuestionScore> pool, int populationSize) {
        List<List<QuestionScore>> population = new ArrayList<>();

        // 策略1：按综合得分排序
        List<QuestionScore> sortedByScore = pool.stream()
                .sorted(Comparator.comparingDouble(QuestionScore::getCompositeScore).reversed())
                .collect(Collectors.toList());
        population.add(selectFrom(sortedByScore));

        // 策略2：先按难度接近度排序
        List<QuestionScore> sortedByDifficulty = pool.stream()
                .sorted(Comparator.comparingDouble(this::getDifficultyNorm).reversed())
                .collect(Collectors.toList());
        population.add(selectFrom(sortedByDifficulty));

        // 策略3：先按区分度排序
        List<QuestionScore> sortedByDiscrimination = pool.stream()
                .sorted(Comparator.comparingDouble(this::getDiscriminationNorm).reversed())
                .collect(Collectors.toList());
        population.add(selectFrom(sortedByDiscrimination));

        // 策略4-30：加随机微扰的排序
        for (int i = 3; i < populationSize; i++) {
            List<QuestionScore> shuffled = new ArrayList<>(pool);
            java.util.Collections.shuffle(shuffled, new java.util.Random(i * 31L));
            population.add(selectFrom(shuffled));
        }

        return population;
    }

    private List<QuestionScore> selectFrom(List<QuestionScore> sorted) {
        List<QuestionScore> selected = new ArrayList<>();
        ConstraintValidator validator = new ConstraintValidator(constraints);
        for (QuestionScore qs : sorted) {
            if (validator.canAdd(selected, qs)) {
                selected.add(qs);
            }
        }
        return selected;
    }

    private double getDifficultyNorm(QuestionScore q) {
        Map<IndicatorEnum, Double> nv = q.getNormalizedValues();
        if (nv != null && nv.containsKey(IndicatorEnum.DIFFICULTY)) {
            return nv.get(IndicatorEnum.DIFFICULTY);
        }
        return 0;
    }

    private double getDiscriminationNorm(QuestionScore q) {
        Map<IndicatorEnum, Double> nv = q.getNormalizedValues();
        if (nv != null && nv.containsKey(IndicatorEnum.DISCRIMINATION)) {
            return nv.get(IndicatorEnum.DISCRIMINATION);
        }
        return 0;
    }

    public record GreedyResult(List<QuestionScore> selected, List<AssemblyDegradeHintDTO> degradeHints, long elapsedMs) {}
}
