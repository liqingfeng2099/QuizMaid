package com.kanade.backend.model.vo;

import lombok.Data;

@Data
public class HighFreqWrongQuestionVO {
    private Long questionId;
    private String questionContent;
    private Integer questionType;
    private String questionTypeName;
    private Integer difficulty;
    private String difficultyName;
    private String knowledgePoints;
    private Long wrongCount;
    private Integer totalScoreLost;
}
