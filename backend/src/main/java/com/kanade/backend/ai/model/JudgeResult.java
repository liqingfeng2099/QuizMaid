package com.kanade.backend.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("简答题批改结果")
public class JudgeResult {

    @Description("总得分")
    Integer totalScore;

    @Description("题目满分")
    Integer maxScore;

    @Description("要点得分明细列表")
    List<ScorePoint> points;

    @Description("整体评语")
    String overallComment;

    @Description("改进建议列表")
    List<String> suggestions;
}
