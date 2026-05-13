package com.kanade.backend.model.vo;

import lombok.Data;

@Data
public class PaperQuestionVO {
    private Long id;               // 关联ID
    private Long questionId;
    private String questionContent; // 可选，题目题干
    private Integer questionScore;
    private Integer sort;
    private Integer type;           // 题型，方便前端显示
    private Integer questionStatus; // 题目状态 0-正常 1-已失效(被逻辑删除)
}