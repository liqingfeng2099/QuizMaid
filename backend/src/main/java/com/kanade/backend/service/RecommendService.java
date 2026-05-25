package com.kanade.backend.service;

import com.kanade.backend.model.dto.RecommendQueryDTO;
import com.kanade.backend.model.vo.QuestionVO;

import java.util.List;

public interface RecommendService {

    List<QuestionVO> recommendSimilarQuestions(RecommendQueryDTO queryDTO);

    void recordFeedback(Long questionId, boolean correct);
}
