package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaperScoreAggregateVO {
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private BigDecimal avgScore;
    private Long totalRecords;
}
