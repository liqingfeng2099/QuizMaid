package com.kanade.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import java.util.List;

@Data
public class ExamSubmitDTO {
    @JsonAlias({"recordId", "record_id"})
    private Long recordId;

    /** 支持两种格式: List<{questionId, userAnswer}> 或 Map<String, String> */
    private Object answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}
