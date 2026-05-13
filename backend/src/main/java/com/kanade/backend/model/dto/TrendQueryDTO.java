package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class TrendQueryDTO {
    private Long userId;
    private String subject;
    private Integer limit;
}
