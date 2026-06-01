package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class RecommendQueryDTO {
    private Integer count = 15;      // 推荐数量 5-30
    private String difficultyTendency = "balanced"; // basic/balanced/advanced
    private Boolean includeAnalysis = true;

    /** 是否过滤"近期已做对"的题目，默认开启 */
    private Boolean filterRecentEnabled = true;
    /** "近期"天数，默认30天，filterRecentEnabled=false 时忽略 */
    private Integer filterRecentDays = 30;
}
