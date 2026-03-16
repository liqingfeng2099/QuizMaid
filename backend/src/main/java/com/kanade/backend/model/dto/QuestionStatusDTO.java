package com.kanade.backend.model.dto;

import lombok.Data;

@Data
public class QuestionStatusDTO {
    private Long id;
    private Integer status; // 1-草稿 2-已发布 3-停用
}