package com.kanade.backend.assembly.model;

import com.kanade.backend.model.entity.Question;

import java.util.Map;

public class QuestionScore {

    private Question question;
    private Map<IndicatorEnum, Double> normalizedValues;
    private double compositeScore;

    public QuestionScore() {}

    public Question getQuestion() { return question; }
    public void setQuestion(Question q) { this.question = q; }

    public Map<IndicatorEnum, Double> getNormalizedValues() { return normalizedValues; }
    public void setNormalizedValues(Map<IndicatorEnum, Double> v) { this.normalizedValues = v; }

    public double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(double s) { this.compositeScore = s; }

    public Long getQuestionId() { return question.getId(); }
    public Integer getDifficulty() { return question.getDifficulty(); }
    public Integer getType() { return question.getType(); }
    public String getSubject() { return question.getSubject(); }
    public String getChapter() { return question.getChapter(); }
    public String getKnowledgePoints() { return question.getKnowledgePoints(); }
    public Long getGradeStageId() { return question.getGradeStageId(); }
    public Integer getCalcLevel() { return question.getCalcLevel(); }
    public Integer getDiscrimination() { return question.getDiscrimination(); }
    public Integer getExamFrequency() { return question.getExamFrequency(); }
}
