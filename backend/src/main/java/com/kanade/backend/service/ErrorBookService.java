package com.kanade.backend.service;

import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.vo.PersonalDimensionVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Map;

public interface ErrorBookService extends IService<ErrorBook> {

    /** 同步错题：考试提交后，将错题录入错题本 */
    void syncFromExam(Long userId, Long questionId);

    /** 分页查询错题 */
    Page<ErrorBook> getErrorPage(int pageNum, int pageSize, Integer errorType,
                                  String knowledgePoint, Integer reviewStatus, String sortBy);

    /** 更新复习状态 */
    void updateReviewStatus(Long id, Integer reviewStatus);

    /** 更新错误类型 */
    void updateErrorType(Long id, Integer errorType);

    /** 归档/取消归档 */
    void toggleArchive(Long id);

    /** 删除错题 */
    void deleteError(Long id);

    /** 错题统计 */
    Map<String, Object> getErrorStats();

    /** 薄弱知识点（雷达图） */
    List<PersonalDimensionVO> getWeakKnowledgePoints();
}
