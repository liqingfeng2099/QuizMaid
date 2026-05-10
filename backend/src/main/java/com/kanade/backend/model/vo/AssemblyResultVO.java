package com.kanade.backend.model.vo;

import com.kanade.backend.model.dto.AssemblyDegradeHintDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyResultVO {

    /**
     * 选中的题目ID列表（按sort排序）
     */
    private List<QuestionScoreVO> questions;

    /**
     * 总题数
     */
    private Integer totalQuestions;

    /**
     * 实际总分
     */
    private Integer actualTotalScore;

    /**
     * 各维度校验结果
     */
    private Map<String, String> dimensionResults;

    /**
     * 适应度分数 (0-1)
     */
    private Double fitness;

    /**
     * 使用的算法类型 GREEDY / GENETIC
     */
    private String algorithmType;

    /**
     * 降级提示（如有题目不足自动降级时填充）
     */
    private List<AssemblyDegradeHintDTO> degradeHints;

    /**
     * 每道题的简要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionScoreVO {
        private Long questionId;
        private Integer type;
        private String content;
        private Integer difficulty;
        private Integer score;
        private Double compositeScore;
    }
}
