package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI组卷结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPaperAssemblyResultVO {
    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 选中的题目ID列表
     */
    private List<Long> questionIds;

    /**
     * 总题数
     */
    private Integer totalQuestions;

    /**
     * 实际总分
     */
    private Integer actualTotalScore;
}
