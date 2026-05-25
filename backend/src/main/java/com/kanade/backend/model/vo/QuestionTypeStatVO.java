package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuestionTypeStatVO {
    private Integer questionType;
    private String questionTypeName;
    private Long totalCount;
    private Long correctCount;
    private BigDecimal correctRate;
    private Integer totalActualScore;
    private Integer totalQuestionScore;
    private BigDecimal scoreRate;
}
