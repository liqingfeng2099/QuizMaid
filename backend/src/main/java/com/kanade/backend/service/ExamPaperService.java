package com.kanade.backend.service;

import com.kanade.backend.model.dto.AIPaperAssemblyDTO;
import com.kanade.backend.model.dto.AIPaperAssemblyV2DTO;
import com.kanade.backend.model.dto.ExamPaperQueryDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.vo.*;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

public interface ExamPaperService extends IService<ExamPaper> {

    Long addExamPaper(ExamPaper examPaper);

    boolean updateExamPaper(ExamPaper examPaper);

    boolean updateStatus(Long id, Integer status);

    ExamPaperVO copyExamPaper(Long id);

    boolean deleteExamPaper(Long id);

    ExamPaperVO getExamPaperVOById(Long id);

    Page<ExamPaperVO> getExamPaperPage(ExamPaperQueryDTO queryDTO);

    AIPaperAssemblyResultVO aiAssemblePaper(AIPaperAssemblyDTO assemblyDTO);

    AIAssemblyStrategyVO aiAssemblePaperV2(AIPaperAssemblyV2DTO dto);

    ExamPaperVO confirmAIAssembly(AIPaperAssemblyV2DTO dto, AIAssemblyStrategyVO strategy);

    AIProfileVO buildUserProfile(Long userId);
}