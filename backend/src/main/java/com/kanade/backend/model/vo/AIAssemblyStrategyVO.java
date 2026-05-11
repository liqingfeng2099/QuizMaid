package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAssemblyStrategyVO {

    private Integer difficultyAvg;
    private List<DifficultyConfig> difficultyConfig;
    private List<TypeConfig> questionTypeConfig;
    private String knowledgePointScope;

    /** AI生成的题目ID列表 */
    private List<Long> questionIds;

    /** 总题数 */
    private Integer totalQuestions;

    /** 实际总分 */
    private Integer actualTotalScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyConfig {
        private Integer level;
        private Double ratio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeConfig {
        private Integer type;
        private Integer count;
        private Integer score;
    }
}
