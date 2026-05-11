package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.annotation.RateLimit;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.model.enums.RateLevel;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.*;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionEsService;
import com.kanade.backend.service.QuestionService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.ByteChunk;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static com.kanade.backend.common.Constant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/question")
@Slf4j
@Tag(name = "试题管理")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionEsService questionEsService;


    // todo 管理员管理题目

    @PostMapping("/add")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L2_MEDIUM)
    @Operation(summary = "添加试题")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddDTO addDTO) {
        Question question = new Question();
        BeanUtils.copyProperties(addDTO, question);

        if (addDTO.getOptions() != null) {
            question.setOptions(addDTO.getOptions().toString());
        }
        if (addDTO.getTags() != null) {
            question.setTags(JSONUtil.toJsonStr(addDTO.getTags()));
        }
        Long id = questionService.addQuestion(question);
        return ResultUtils.success(id);
    }

    @PostMapping("/add/batch")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L2_MEDIUM)
    @Operation(summary = "批量添加试题")
    public BaseResponse<List<Long>> batchAddQuestion(@RequestBody List<QuestionAddDTO> batchAddDTOList) {
        if (CollUtil.isEmpty(batchAddDTOList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<Question> questionList = new ArrayList<>();
        for (QuestionAddDTO addDTO : batchAddDTOList) {
            Question question = new Question();
            BeanUtils.copyProperties(addDTO, question);
            // 选项、标签格式转换
            if (addDTO.getOptions() != null) {
                question.setOptions(addDTO.getOptions().toString());
            }
            if (addDTO.getTags() != null) {
                question.setTags(JSONUtil.toJsonStr(addDTO.getTags()));
            }
            questionList.add(question);
        }
        // 3. 调用批量插入服务
        List<Long> questionIdList = questionService.batchAddQuestion(questionList);
        return ResultUtils.success(questionIdList);
    }

    @PostMapping("/update")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L2_MEDIUM)
    @Operation(summary = "更新试题")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateDTO updateDTO) {
        if (updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试题ID不能为空");
        }
        log.info("dto ", updateDTO);
        Question question = new Question();
        BeanUtils.copyProperties(updateDTO, question);
        log.info("por ", question);
        boolean result = questionService.updateQuestion(question);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L2_MEDIUM)
    @Operation(summary = "逻辑删除试题")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试题ID不能为空");
        }
        User user =(User) StpUtil.getSession().get(USER_LOGIN_STATE);
        Question byId = questionService.getById(deleteRequest.getId());

        if (!user.getRole().equals("admin") && !user.getId().equals(byId.getCreatorId())){
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,"无权访问");
        }
        boolean result = questionService.deleteQuestion(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/status")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L2_MEDIUM)
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
    @RateLimit(level = RateLevel.L3_LOOSE)
    @Operation(summary = "根据ID获取试题详情")
    public BaseResponse<QuestionVO> getQuestionById(@PathVariable Long id) {
        QuestionVO vo = questionService.getQuestionVOById(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/list/admin/page")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L3_LOOSE)
    @Operation(summary = "分页查询试题")
    @SaCheckRole("admin")
    public BaseResponse<Page<QuestionVO>> listAllQuestionByPage(@RequestBody QuestionQueryDTO queryDTO) {
        Page<QuestionVO> page = questionService.getQuestionPage(queryDTO);
        return ResultUtils.success(page);
    }

    // todo 用户查看自己上传的题目
    @PostMapping("/list/page")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L3_LOOSE)
    @Operation(summary = "分页查询试题")
    public BaseResponse<Page<QuestionVO>> listQuestionByPage(@RequestBody QuestionQueryDTO queryDTO) {
        queryDTO.setCreatorId(StpUtil.getLoginIdAsLong());
        Page<QuestionVO> page = questionService.getQuestionPage(queryDTO);
        return ResultUtils.success(page);
    }

    @PostMapping("/search")
    @SaCheckLogin
    @RateLimit(level = RateLevel.L2_MEDIUM)
    @Operation(summary = "ES全文检索试题（支持分词）")
    public BaseResponse<Page<QuestionVO>> searchQuestions(@RequestParam String keyword, @RequestBody QuestionQueryDTO queryDTO) {
        Page<QuestionVO> page = questionEsService.searchQuestions(keyword, queryDTO);
        return ResultUtils.success(page);
    }
}