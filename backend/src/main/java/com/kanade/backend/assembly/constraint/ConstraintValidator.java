package com.kanade.backend.assembly.constraint;

import com.kanade.backend.assembly.model.AssemblyConstraint;
import com.kanade.backend.assembly.model.QuestionScore;
import com.kanade.backend.model.dto.AssemblyDegradeHintDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConstraintValidator {

    private final AssemblyConstraint constraints;
    private final int degradeLevel;
    private final List<AssemblyDegradeHintDTO> degradeHints = new ArrayList<>();

    /**
     * 降级优先级：题型数量 > 总分值 > 难度范围 > 知识点分布 > 考频 > 计算量
     */
    private static final String[] DEGRADE_PRIORITY = {
            "typeCount", "totalScore", "difficultyRange", "knowledgePoint", "examFrequency", "calcLevel"
    };

    public ConstraintValidator(AssemblyConstraint constraints) {
        this.constraints = constraints;
        this.degradeLevel = 0;
    }

    public ConstraintValidator(AssemblyConstraint constraints, int degradeLevel) {
        this.constraints = constraints;
        this.degradeLevel = degradeLevel;
    }

    /**
     * 校验一道题能否加入当前试卷
     */
    public boolean canAdd(List<QuestionScore> currentPaper, QuestionScore candidate) {
        List<QuestionScore> hypothetical = new ArrayList<>(currentPaper);
        hypothetical.add(candidate);
        return validate(hypothetical, false);
    }

    /**
     * 校验整份试卷是否满足硬约束
     */
    public boolean validate(List<QuestionScore> paper, boolean strict) {
        if (constraints == null) return true;

        int n = paper.size();
        if (n == 0) return true;

        // 题型约束校验 (hard - can be relaxed)
        if (constraints.getTypeConstraints() != null && !constraints.getTypeConstraints().isEmpty()) {
            Map<Integer, Long> actualTypeCount = paper.stream()
                    .collect(Collectors.groupingBy(QuestionScore::getType, Collectors.counting()));

            int relaxedBy = Math.max(0, degradeLevelFor("typeCount"));
            for (Map.Entry<Integer, AssemblyConstraint.TypeConstraint> entry : constraints.getTypeConstraints().entrySet()) {
                long actual = actualTypeCount.getOrDefault(entry.getKey(), 0L);
                int expected = entry.getValue().getCount() - relaxedBy;
                if (strict && actual < expected && expected > 0) {
                    return false;
                }
            }
        }

        // 总分约束 (hard - can be relaxed)
        if (constraints.getTargetTotalScore() != null) {
            int actualTotal = paper.stream().mapToInt(q -> 10).sum();
            int relaxed = constraints.getTargetTotalScore() - degradeLevelFor("totalScore") * 10;
            if (strict && actualTotal < relaxed) {
                return false;
            }
        }

        return true;
    }

    /**
     * 校验整卷并返回降级提示
     */
    public List<AssemblyDegradeHintDTO> validateWithHints(List<QuestionScore> paper) {
        degradeHints.clear();
        boolean passed = validate(paper, true);

        if (!passed) {
            // 自动降级
            int level = 1;
            while (!validateWithDegrade(paper, level) && level < 3) {
                level++;
            }
            validateWithDegrade(paper, level);
        }

        return new ArrayList<>(degradeHints);
    }

    private boolean validateWithDegrade(List<QuestionScore> paper, int level) {
        int n = paper.size();

        if (constraints.getTypeConstraints() != null) {
            Map<Integer, Long> actualTypeCount = paper.stream()
                    .collect(Collectors.groupingBy(QuestionScore::getType, Collectors.counting()));
            for (Map.Entry<Integer, AssemblyConstraint.TypeConstraint> entry : constraints.getTypeConstraints().entrySet()) {
                long actual = actualTypeCount.getOrDefault(entry.getKey(), 0L);
                int expected = entry.getValue().getCount() - level;
                if (actual < expected && expected > 0 && level > 0) {
                    degradeHints.add(AssemblyDegradeHintDTO.builder()
                            .degradedIndicator("typeCount")
                            .originalConstraint("题型" + entry.getKey() + "需" + entry.getValue().getCount() + "题")
                            .degradedConstraint("放宽至" + Math.max(0, expected) + "题")
                            .reason("候选题目不足，题型数量自动降级")
                            .build());
                }
            }
        }

        if (constraints.getTargetTotalScore() != null) {
            int actualTotal = paper.stream().mapToInt(q -> 10).sum();
            int relaxed = constraints.getTargetTotalScore() - level * 10;
            if (actualTotal < relaxed && level > 0) {
                degradeHints.add(AssemblyDegradeHintDTO.builder()
                        .degradedIndicator("totalScore")
                        .originalConstraint("目标总分" + constraints.getTargetTotalScore())
                        .degradedConstraint("放宽至" + Math.max(0, relaxed))
                        .reason("候选题目不足，总分要求自动降级")
                        .build());
            }
        }

        return !degradeHints.isEmpty();
    }

    private int degradeLevelFor(String indicator) {
        return Math.max(0, degradeLevel - getPriorityIndex(indicator));
    }

    private int getPriorityIndex(String indicator) {
        for (int i = 0; i < DEGRADE_PRIORITY.length; i++) {
            if (DEGRADE_PRIORITY[i].equals(indicator)) return i;
        }
        return DEGRADE_PRIORITY.length;
    }

    public List<AssemblyDegradeHintDTO> getDegradeHints() {
        return degradeHints;
    }
}
