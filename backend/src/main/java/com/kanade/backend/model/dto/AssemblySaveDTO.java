package com.kanade.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssemblySaveDTO {
    @NotBlank(message = "试卷名称不能为空")
    private String paperName;

    private String subject;

    @NotNull(message = "试卷状态不能为空")
    private Integer status;

    private Long strategyId;

    @NotEmpty(message = "题目列表不能为空")
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {
        @NotNull
        private Long questionId;

        @NotNull
        private Integer questionScore;

        private Integer sort;
    }
}
