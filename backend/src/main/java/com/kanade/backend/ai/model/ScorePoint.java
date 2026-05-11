package com.kanade.backend.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("单个得分点的评分详情")
public class ScorePoint {
    @Description("要点名称")
    String pointName;

    @Description("该要点满分")
    Integer maxPointScore;

    @Description("实际得分")
    Integer actualScore;

    @Description("得分原因或扣分说明")
    String comment;
}
