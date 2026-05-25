package com.kanade.backend.service;

import com.kanade.backend.model.dto.StatisticsQueryDTO;
import com.kanade.backend.model.dto.TrendQueryDTO;
import com.kanade.backend.model.vo.PaperStatisticsVO;
import com.kanade.backend.model.vo.TrendDataPointVO;

import java.util.List;

public interface StatisticsService {

    PaperStatisticsVO getPaperStatistics(StatisticsQueryDTO queryDTO);

    List<TrendDataPointVO> getScoreTrend(TrendQueryDTO queryDTO);

    PaperStatisticsVO getComparison(Long paperId);

    List<PaperStatisticsVO> getAvailablePapers();
}
