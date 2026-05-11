package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperStrategyVO {

    private Long id;
    private String strategyName;
    private Long userId;
    private Integer totalScore;
    private Integer difficultyAvg;
    private Integer duration;
    private String questionTypeConfig;
    private String difficultyConfig;
    private String knowledgePointScope;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 关联的权重列表
     */
    private List<StrategyWeightVO> weights;

    /**
     * 权重总和（校验用）
     */
    private Integer weightSum;
}
