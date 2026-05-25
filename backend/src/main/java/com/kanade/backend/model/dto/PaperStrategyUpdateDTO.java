package com.kanade.backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PaperStrategyUpdateDTO {

    @NotNull(message = "策略ID不能为空")
    private Long id;

    private String strategyName;

    @Min(1) @Max(500)
    private Integer totalScore;

    @Min(1) @Max(5)
    private Integer difficultyAvg;

    @Min(1) @Max(300)
    private Integer duration;

    private String questionTypeConfig;
    private String difficultyConfig;
    private String knowledgePointScope;

    @Size(min = 6, max = 6)
    private List<StrategyWeightDTO> weights;
}
