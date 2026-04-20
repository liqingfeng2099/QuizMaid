package com.kanade.backend.model.dto;

import lombok.Data;

/**
 * AI组卷请求DTO
 */
@Data
public class AIPaperAssemblyDTO {
    /**
     * 试卷名称（必填）
     */
    private String paperName;

    /**
     * 所属科目（可选）
     */
    private String subject;

    /**
     * 章节（可选）
     */
    private String chapter;

    /**
     * 难度（可选，1-易 2-中 3-难）
     */
    private Integer difficulty;

    /**
     * 总分（可选）
     */
    private Integer totalScore;

    /**
     * 状态（必填，0-草稿 1-已发布 2-已归档 3-已停用）
     */
    private Integer status;

    /**
     * 用户自定义需求描述
     */
    private String userRequirement;
}
