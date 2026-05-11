package com.kanade.backend.assembly.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AssemblyConstraint {

    private Integer targetTotalScore;
    private Integer targetDifficultyAvg;
    private Long gradeStageId;

    /**
     * 题型 → {count, scorePerQuestion}
     * e.g. type=1 → {count: 10, score: 5}
     */
    private Map<Integer, TypeConstraint> typeConstraints;

    /**
     * 难度级别 → 占比
     * e.g. level=1 → 0.2
     */
    private Map<Integer, Double> difficultyRatios;

    @Data
    @Builder
    public static class TypeConstraint {
        private int count;
        private int scorePerQuestion;
    }
}
