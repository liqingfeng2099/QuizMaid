package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.*;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question")
@Slf4j
@Tag(name = "试题管理")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "添加试题")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddDTO addDTO) {
        Question question = new Question();
        BeanUtils.copyProperties(addDTO, question);
        Long id = questionService.addQuestion(question);
        return ResultUtils.success(id);
    }

    @PostMapping("/update")
    @SaCheckLogin
    @Operation(summary = "更新试题")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateDTO updateDTO) {
        if (updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试题ID不能为空");
        }
        Question question = new Question();
        BeanUtils.copyProperties(updateDTO, question);
        boolean result = questionService.updateQuestion(question);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @Operation(summary = "逻辑删除试题")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试题ID不能为空");
        }
        boolean result = questionService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/status")
    @SaCheckLogin
    @Operation(summary = "修改试题状态")
    public BaseResponse<Boolean> updateStatus(@RequestBody QuestionStatusDTO statusDTO) {
        if (statusDTO.getId() == null || statusDTO.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        boolean result = questionService.updateStatus(statusDTO.getId(), statusDTO.getStatus());
        return ResultUtils.success(result);
    }

    @GetMapping("/get/{id}")
    @SaCheckLogin
    @Operation(summary = "根据ID获取试题详情")
    public BaseResponse<QuestionVO> getQuestionById(@PathVariable Long id) {
        QuestionVO vo = questionService.getQuestionVOById(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/list/page")
    @SaCheckLogin
    @Operation(summary = "分页查询试题")
    public BaseResponse<Page<QuestionVO>> listQuestionByPage(@RequestBody QuestionQueryDTO queryDTO) {
        Page<QuestionVO> page = questionService.getQuestionPage(queryDTO);
        return ResultUtils.success(page);
    }
}