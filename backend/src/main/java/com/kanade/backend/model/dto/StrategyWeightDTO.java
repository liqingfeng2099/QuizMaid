package com.kanade.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StrategyWeightDTO {

    @NotBlank(message = "权重类型不能为空")
    private String weightType;

    @NotNull(message = "权重值不能为空")
    @Min(0) @Max(100)
    private Integer weightValue;
}
