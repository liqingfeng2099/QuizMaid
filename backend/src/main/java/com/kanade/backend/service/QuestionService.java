package com.kanade.backend.service;

import com.kanade.backend.model.dto.QuestionQueryDTO;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.QuestionVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

public interface QuestionService extends IService<Question> {
    /**
     * 添加试题（自动生成MD5）
     */
    Long addQuestion(Question question);

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

    // 原有的 AI 标注方法保留
    Question addLabels(Question question);
}