package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class QuestionAddDTO {
    /**
     * 题型 1-单选 2-多选 3-填空 4-简答
     */
    private Integer type;

    /**
     * 学科
     */
    private String subject;

    /**
     * 章节
     */
    private String chapter;

    /**
     * 难度 1-易 2-中 3-难
     */
    private Integer difficulty;

    /**
     * 知识点，逗号分隔
     */
    private String knowledgePoints;

    /**
     * 题目标签（JSON数组字符串）
     */
    private String tags;

    /**
     * 题干内容
     */
    private String content;

    /**
     * 选项JSON（字符串）
     */
    private String options;

    /**
     * 标准答案
     */
    private String answer;

    /**
     * 解析
     */
    private String analysis;

    /**
     * 状态 1-草稿 2-已发布 3-停用
     */
    private Integer status;
}