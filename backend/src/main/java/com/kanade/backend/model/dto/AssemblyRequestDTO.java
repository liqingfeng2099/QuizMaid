package com.kanade.backend.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssemblyRequestDTO {

    @NotNull(message = "组卷策略ID不能为空")
    private Long strategyId;

    /**
     * 可选：进一步限定学科
     */
    private String subject;

    /**
     * 可选：进一步限定章节
     */
    private String chapter;

    /**
     * 可选：试卷名称
     */
    private String paperName;

    /**
     * 可选：试卷状态
     */
    private Integer paperStatus;
}
