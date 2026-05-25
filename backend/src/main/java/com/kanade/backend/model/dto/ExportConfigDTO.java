package com.kanade.backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExportConfigDTO {
    private Long paperId;
    private List<Long> paperIds;
    private Boolean showAnswer;
    private Boolean showAnalysis;
    private String titleAlign;
    private String scorePosition;
    private String exportType;
    private Long templateId;
    private Boolean editable;    // PDF可编辑模式(含表单填空域)
}
