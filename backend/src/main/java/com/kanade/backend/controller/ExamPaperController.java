package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.AIPaperAssemblyDTO;
import com.kanade.backend.model.dto.ExamPaperAddDTO;
import com.kanade.backend.model.dto.ExamPaperQueryDTO;
import com.kanade.backend.model.dto.ExamPaperStatusDTO;
import com.kanade.backend.model.dto.ExamPaperUpdateDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.vo.AIPaperAssemblyResultVO;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.kanade.backend.service.ExamPaperService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/examPaper")
@RequiredArgsConstructor
@Tag(name = "试卷管理")
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "添加试卷")
    public BaseResponse<Long> addExamPaper(@RequestBody ExamPaperAddDTO addDTO) {
        ExamPaper paper = new ExamPaper();
        BeanUtils.copyProperties(addDTO, paper);
        Long id = examPaperService.addExamPaper(paper);
        return ResultUtils.success(id);
    }

    @PostMapping("/update")
    @SaCheckLogin
    @Operation(summary = "更新试卷")
    public BaseResponse<Boolean> updateExamPaper(@RequestBody ExamPaperUpdateDTO updateDTO) {
        if (updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        ExamPaper paper = new ExamPaper();
        BeanUtils.copyProperties(updateDTO, paper);
        boolean result = examPaperService.updateExamPaper(paper);
        return ResultUtils.success(result);
    }

    @PostMapping("/status")
    @SaCheckLogin
    @Operation(summary = "修改试卷状态")
    public BaseResponse<Boolean> updateStatus(@RequestBody ExamPaperStatusDTO statusDTO) {
        if (statusDTO.getId() == null || statusDTO.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        boolean result = examPaperService.updateStatus(statusDTO.getId(), statusDTO.getStatus());
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @Operation(summary = "逻辑删除试卷")
    public BaseResponse<Boolean> deleteExamPaper(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        boolean result = examPaperService.deleteExamPaper(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/get/{id}")
    @SaCheckLogin
    @Operation(summary = "根据ID获取试卷详情（含试题列表）")
    public BaseResponse<ExamPaperVO> getExamPaperById(@PathVariable Long id) {
        ExamPaperVO vo = examPaperService.getExamPaperVOById(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/list/page")
    @SaCheckLogin
    @Operation(summary = "分页查询试卷（普通用户只能看到自己的，管理员可查所有）")
    public BaseResponse<Page<ExamPaperVO>> listExamPaperByPage(@RequestBody ExamPaperQueryDTO queryDTO) {
        Page<ExamPaperVO> page = examPaperService.getExamPaperPage(queryDTO);
        return ResultUtils.success(page);
    }

    @PostMapping("/ai/assemble")
    @SaCheckLogin // ← Sa-Token拦截器验证登录状态
    @Operation(summary = "AI智能组卷")
    public BaseResponse<AIPaperAssemblyResultVO> aiAssemblePaper(@RequestBody AIPaperAssemblyDTO assemblyDTO) {
        // 调用Service层
        AIPaperAssemblyResultVO result = examPaperService.aiAssemblePaper(assemblyDTO);
        return ResultUtils.success(result);
    }
}