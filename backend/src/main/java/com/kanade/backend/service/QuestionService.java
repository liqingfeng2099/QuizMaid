package com.kanade.backend.service;

import com.kanade.backend.model.dto.QuestionQueryDTO;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.QuestionVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface QuestionService extends IService<Question> {
    /**
     * 添加试题（自动生成MD5）
     */
    Long addQuestion(Question question);

    Long addQuestion(Question question, Long creatorId);
    /**
     * 更新试题（重新生成MD5并查重）
     */
    boolean updateQuestion(Question question);

    /**
     * 分页查询试题（返回VO）
     */
    Page<QuestionVO> getQuestionPage(QuestionQueryDTO queryDTO);

    /**
     * 根据ID获取试题详情（返回VO）
     */
    QuestionVO getQuestionVOById(Long id);

    /**
     * 修改试题状态
     */
    boolean updateStatus(Long id, Integer status);

    List<Long> batchAddQuestion(List<Question> questionList);

    boolean deleteQuestion(Long id);
}