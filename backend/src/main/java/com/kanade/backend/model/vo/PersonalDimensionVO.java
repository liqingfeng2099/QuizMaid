package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PersonalDimensionVO {
    private String dimensionKey;    // 维度值（如"单选题"/"简单"/"二次函数"）
    private Long totalCount;
    private Long correctCount;
    private BigDecimal correctRate;
    private BigDecimal scoreRate;
}
