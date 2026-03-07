package com.kanade.backend.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
@Description("题库知识点标注的结果")
public class LabelResult {

    @Description("知识点集合")
    private List<String> knowledgePoints;

    @Description("所属学科")
    private String subject;

    @Description("难度等级")
    Integer difficult;
}
