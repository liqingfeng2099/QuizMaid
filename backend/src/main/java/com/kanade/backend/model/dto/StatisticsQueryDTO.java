package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class StatisticsQueryDTO {
    private Long paperId;
    private String subject;
    private Integer paperType;
    private String startTime;
    private String endTime;
}
