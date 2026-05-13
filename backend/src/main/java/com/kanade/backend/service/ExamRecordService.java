package com.kanade.backend.service;

import com.kanade.backend.model.vo.ExamRecordVO;
import com.kanade.backend.model.vo.ExamResultVO;
import com.kanade.backend.model.entity.Userexamrecord;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Map;

public interface ExamRecordService extends IService<Userexamrecord> {

    /** 开始考试：创建考试记录并返回试题（不含答案） */
    ExamRecordVO startExam(Long paperId);

    /** 提交考试：保存答案并自动批改客观题 */
    ExamResultVO submitExam(Long recordId, Map<Long, String> answers);

    /** 超时自动交卷 */
    ExamResultVO autoSubmit(Long recordId);

    /** 获取考试结果 */
    ExamResultVO getExamResult(Long recordId);

    /** 获取用户的考试记录列表 */
    List<ExamRecordVO> getUserExamRecords(Long userId, Long paperId);

    /** 获取进行中的考试 */
    ExamRecordVO getOngoingExam(Long paperId);
}
