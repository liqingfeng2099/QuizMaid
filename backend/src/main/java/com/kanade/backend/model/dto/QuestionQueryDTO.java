package com.kanade.backend.model.dto;

import com.kanade.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionQueryDTO extends PageRequest {
    private Long id;
    private Integer type;
    private String subject;
    private String chapter;
    private Integer difficulty;
    private String knowledgePoints; // 模糊查询
    private String tags;             // 模糊查询
    private String content;          // 模糊查询
    private Long creatorId;
    private Integer status;           // 精确匹配
}