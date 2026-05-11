package com.kanade.backend.service;

import com.kanade.backend.model.vo.QuestionVO;
import com.mybatisflex.core.paginate.Page;
import org.redisson.api.RLock;

public interface QuestionCacheService {

    Page<QuestionVO> getCachedQuestionList(String cacheKey);

    void cacheQuestionList(String cacheKey, Page<QuestionVO> voPage);

    QuestionVO getCachedQuestionDetail(Long id);

    void cacheQuestionDetail(Long id, QuestionVO vo);

    void removeQuestionDetailCache(Long id);

    void recordQuestionAccess(Long id);

    RLock getDetailLock(Long id);
}
