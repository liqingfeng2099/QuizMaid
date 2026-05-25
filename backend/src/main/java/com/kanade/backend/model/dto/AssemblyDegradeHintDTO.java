package com.kanade.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyDegradeHintDTO {

    /**
     * 被降级的指标类型
     */
    private String degradedIndicator;

    /**
     * 原约束值
     */
    private String originalConstraint;

    /**
     * 降级后约束值
     */
    private String degradedConstraint;

    /**
     * 降级原因
     */
    private String reason;
}
