package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class RecommendQueryDTO {
    private Integer count = 15;      // 推荐数量 10-20
    private String difficultyTendency = "balanced"; // basic/balanced/advanced
    private Boolean includeAnalysis = true;
}
