package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaperStatisticsVO {
    private Long paperId;
    private String paperName;
    private String subject;
    private Integer totalScore;
    private Long creatorId;
    private Integer status;
    private Integer paperType;
    private String createTime;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private BigDecimal avgScore;
    private BigDecimal medianScore;
    private Integer totalExaminees;
    private BigDecimal highScoreRate;
    private BigDecimal passRate;
    private List<ScoreDistributionVO> scoreDistribution;
    private List<QuestionTypeStatVO> questionTypeStats;
    private List<DifficultyStatVO> difficultyStats;
    private List<KnowledgePointStatVO> knowledgePointStats;
    private List<HighFreqWrongQuestionVO> highFreqWrongQuestions;
    private LocalDateTime calculationTimestamp;
}
