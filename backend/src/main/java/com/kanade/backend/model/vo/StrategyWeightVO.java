package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyWeightVO {
    private Long id;
    private Long strategyId;
    private String weightType;
    private Integer weightValue;
}
