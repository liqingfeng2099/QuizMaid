package com.kanade.backend.model.vo;

import lombok.Data;

@Data
public class ExamQuestionItem {
    private Long questionId;
    private Integer type;         // 1-单选 2-多选 3-填空 4-简答
    private String content;       // 题干
    private String options;       // 选项JSON
    private Integer score;        // 分值
    private Integer sort;         // 排序
    private String userAnswer;    // 用户答案（提交后）
    private Integer correctStatus; // 0-待批改 1-正确 2-错误
    private Integer actualScore;  // 实际得分
    private String correctAnswer; // 正确答案（考试结束后可见）
    private String analysis;      // 解析（考试结束后可见）
}
