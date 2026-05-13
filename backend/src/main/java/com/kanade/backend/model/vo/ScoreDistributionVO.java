package com.kanade.backend.model.vo;

import lombok.Data;

@Data
public class ScoreDistributionVO {
    private Integer scoreBucket;
    private Long count;
}
