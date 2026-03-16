package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuestionVO {
    private Long id;
    private String questionMd5;
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
    private Long creatorId;
    private Integer status;
    private Long correctCount;
    private Long totalCount;
    private BigDecimal accuracy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}