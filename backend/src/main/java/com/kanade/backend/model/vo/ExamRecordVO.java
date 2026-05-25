package com.kanade.backend.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamRecordVO {
    private Long recordId;
    private Long paperId;
    private String paperName;
    private Integer totalScore;
    private Integer userScore;
    private Integer status;        // 0-未完成 1-已完成 2-已批改
    private String durationText;   // 答题时长（如"45分钟"）
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long remainingSeconds; // 剩余秒数（进行中时有效）
    private Integer totalQuestions;
    private List<ExamQuestionItem> questions;
}
