package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class QuestionUpdateDTO {
    private Long id;

    private Integer type;
    private String subject;
    private String chapter;
    private Integer difficulty;
    private String knowledgePoints;
    private String tags;
    private String content;
    private String options;
    private String answer;
    private String analysis;
    private Integer status;
}