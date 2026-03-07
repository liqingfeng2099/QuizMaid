package com.kanade.backend.service;

import com.mybatisflex.core.service.IService;
import com.kanade.backend.model.entity.Question;

/**
 * 试题主表 服务层。
 *
 * @author kanade
 */
public interface QuestionService extends IService<Question> {
    Question addLabels(Question question);
}
