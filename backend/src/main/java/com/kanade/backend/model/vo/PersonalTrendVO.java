package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PersonalTrendVO {
    private String period;        // 日期或周期标签
    private Long answerCount;
    private Long correctCount;
    private BigDecimal accuracy;
}
