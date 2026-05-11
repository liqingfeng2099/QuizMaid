package com.kanade.backend.assembly.model;

import java.util.ArrayList;
import java.util.List;

public class PaperCandidate {

    private List<Long> questionIds = new ArrayList<>();
    private List<QuestionScore> questionScores = new ArrayList<>();
    private double fitness;
    private int totalScore;
    private double avgDifficulty;
    private double avgAccuracy;
    private double avgDiscrimination;
    private double avgCalcLevel;
    private double avgExamFrequency;
    private double avgKnowledgeCount;

    public PaperCandidate() {}

    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> ids) { this.questionIds = ids; }

    public List<QuestionScore> getQuestionScores() { return questionScores; }
    public void setQuestionScores(List<QuestionScore> scores) { this.questionScores = scores; }

    public double getFitness() { return fitness; }
    public void setFitness(double f) { this.fitness = f; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int s) { this.totalScore = s; }

    public double getAvgDifficulty() { return avgDifficulty; }
    public void setAvgDifficulty(double v) { this.avgDifficulty = v; }

    public double getAvgAccuracy() { return avgAccuracy; }
    public void setAvgAccuracy(double v) { this.avgAccuracy = v; }

    public double getAvgDiscrimination() { return avgDiscrimination; }
    public void setAvgDiscrimination(double v) { this.avgDiscrimination = v; }

    public double getAvgCalcLevel() { return avgCalcLevel; }
    public void setAvgCalcLevel(double v) { this.avgCalcLevel = v; }

    public double getAvgExamFrequency() { return avgExamFrequency; }
    public void setAvgExamFrequency(double v) { this.avgExamFrequency = v; }

    public double getAvgKnowledgeCount() { return avgKnowledgeCount; }
    public void setAvgKnowledgeCount(double v) { this.avgKnowledgeCount = v; }

    public int size() { return questionIds.size(); }

    public PaperCandidate copy() {
        PaperCandidate c = new PaperCandidate();
        c.questionIds = new ArrayList<>(this.questionIds);
        c.questionScores = new ArrayList<>(this.questionScores);
        c.fitness = this.fitness;
        c.totalScore = this.totalScore;
        c.avgDifficulty = this.avgDifficulty;
        c.avgAccuracy = this.avgAccuracy;
        c.avgDiscrimination = this.avgDiscrimination;
        c.avgCalcLevel = this.avgCalcLevel;
        c.avgExamFrequency = this.avgExamFrequency;
        c.avgKnowledgeCount = this.avgKnowledgeCount;
        return c;
    }
}
