package com.kanade.backend.service;

import com.kanade.backend.model.dto.QuestionQueryDTO;
import com.kanade.backend.model.es.QuestionDocument;
import com.kanade.backend.model.vo.QuestionVO;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

public interface QuestionEsService {

    void syncQuestionToEs(QuestionDocument document);

    void deleteQuestionFromEs(Long id);

    Page<QuestionVO> searchQuestions(String keyword, QuestionQueryDTO queryDTO);

    void batchSyncQuestionsToEs(List<QuestionDocument> documents);
}
