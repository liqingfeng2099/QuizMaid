package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TrendDataPointVO {
    private Long userId;
    private Long paperId;
    private String paperName;
    private Integer score;
    private BigDecimal scoreRate;
    private LocalDateTime examTime;
}
