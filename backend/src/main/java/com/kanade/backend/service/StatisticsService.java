package com.kanade.backend.service;

import com.kanade.backend.model.dto.StatisticsQueryDTO;
import com.kanade.backend.model.dto.TrendQueryDTO;
import com.kanade.backend.model.vo.PaperStatisticsVO;
import com.kanade.backend.model.vo.QuestionTypeStatVO; // 题型正确率统计VO .hml
import com.kanade.backend.model.vo.TrendDataPointVO;

import java.util.List;

public interface StatisticsService {

    PaperStatisticsVO getPaperStatistics(StatisticsQueryDTO queryDTO);

    List<TrendDataPointVO> getScoreTrend(TrendQueryDTO queryDTO);

    PaperStatisticsVO getComparison(Long paperId);

    List<PaperStatisticsVO> getAvailablePapers();

    // 获取全部题型正确率统计 .hml
    List<QuestionTypeStatVO> getTypeAccuracy();

    // 获取指定试卷的题型正确率统计 .hml
    List<QuestionTypeStatVO> getPaperTypeAccuracy(Long paperId);
}
