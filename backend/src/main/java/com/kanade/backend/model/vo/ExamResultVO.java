package com.kanade.backend.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamResultVO {
    private Long recordId;
    private Long paperId;
    private String paperName;
    private String subject;
    private Integer totalScore;
    private Integer userScore;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long usedSeconds;     // 实际答题用时（秒）
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer pendingCount; // 待批改数量（主观题）
    private List<ExamQuestionItem> questions;
}
