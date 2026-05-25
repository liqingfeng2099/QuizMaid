package com.kanade.backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PaperStrategyAddDTO {

    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    @NotNull(message = "目标总分不能为空")
    @Min(1) @Max(500)
    private Integer totalScore;

    @NotNull(message = "平均难度不能为空")
    @Min(1) @Max(5)
    private Integer difficultyAvg;

    @Min(1) @Max(300)
    private Integer duration;

    private String questionTypeConfig;
    private String difficultyConfig;
    private String knowledgePointScope;

    @NotNull(message = "权重配置不能为空")
    @Size(min = 6, max = 6, message = "必须恰好配置6个指标的权重")
    private List<StrategyWeightDTO> weights;
}
