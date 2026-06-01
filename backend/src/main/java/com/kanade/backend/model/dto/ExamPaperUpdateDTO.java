package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class ExamPaperUpdateDTO {
    private Long id;
    private String paperName;
    private String subject;
    private Integer totalScore;
    private Integer duration; // 答题时长（分钟）
}