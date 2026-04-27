package com.kanade.backend.service;

import com.kanade.backend.model.dto.AIPaperAssemblyDTO;
import com.kanade.backend.model.dto.ExamPaperQueryDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.vo.AIPaperAssemblyResultVO;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

public interface ExamPaperService extends IService<ExamPaper> {

    Long addExamPaper(ExamPaper examPaper);

    boolean updateExamPaper(ExamPaper examPaper);

    boolean updateStatus(Long id, Integer status);

    boolean deleteExamPaper(Long id);

    ExamPaperVO getExamPaperVOById(Long id);

    Page<ExamPaperVO> getExamPaperPage(ExamPaperQueryDTO queryDTO);

    /**
     * AI智能组卷
     */
    AIPaperAssemblyResultVO aiAssemblePaper(AIPaperAssemblyDTO assemblyDTO);
}