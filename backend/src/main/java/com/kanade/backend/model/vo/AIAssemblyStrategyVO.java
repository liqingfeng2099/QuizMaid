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

    /** 关联的策略ID（A+C模式下持久化的 PaperStrategy.id） */
    private Long strategyId;

    /** 策略描述（LLM推断的一句话摘要） */
    private String strategyDescription;

    /** 组卷阶段详情（用于前端展示流程） */
    private String stageDetail;

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
