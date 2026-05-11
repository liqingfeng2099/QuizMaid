package com.kanade.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIPaperAssemblyV2DTO {

    @NotBlank(message = "试卷名称不能为空")
    private String paperName;

    private String subject;
    private String chapter;
    private Integer difficulty;
    private Integer totalScore;

    @NotNull(message = "试卷状态不能为空")
    private Integer status;

    private String userRequirement;

    /** 是否融入个性化学习数据 */
    private Boolean usePersonalization;

    /** 是否聚焦薄弱知识点组卷 */
    private Boolean includeWeakAreas;

    /** 历史对话ID（续写对话） */
    private Long previousChatId;
}
