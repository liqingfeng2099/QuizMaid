package com.kanade.backend.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PersonalStatsVO {
    private Long totalAnswers;
    private Long totalCorrect;
    private BigDecimal totalAccuracy;
    private List<PersonalDimensionVO> byType;        // 按题型
    private List<PersonalDimensionVO> byDifficulty;  // 按难度
    private List<PersonalDimensionVO> byKnowledge;   // 按知识点
    private List<PersonalTrendVO> trend;             // 趋势数据
}
