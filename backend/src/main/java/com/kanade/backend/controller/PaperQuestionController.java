package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.PaperQuestionAddDTO;
import com.kanade.backend.model.dto.PaperQuestionUpdateDTO;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.service.CounterManager;
import com.kanade.backend.service.PaperQuestionService;
import com.kanade.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/paperQuestion")
@RequiredArgsConstructor
@Tag(name = "试卷试题关联管理")
public class PaperQuestionController {
    @Resource
    UserService userService;
    @Resource
    private CounterManager counterManager;

    private final PaperQuestionService paperQuestionService;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "添加试题到试卷")
    public BaseResponse<Long> addQuestionToPaper(@RequestBody PaperQuestionAddDTO addDTO) {
        Long id = paperQuestionService.addQuestionToPaper(addDTO);
        return ResultUtils.success(id);
    }

    @PostMapping("/update")
    @SaCheckLogin
    @Operation(summary = "更新试卷中试题的分值或排序")
    public BaseResponse<Boolean> updatePaperQuestion(@RequestBody PaperQuestionUpdateDTO updateDTO) {
        boolean result = paperQuestionService.updatePaperQuestion(updateDTO);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @Operation(summary = "从试卷中移除试题")
    public BaseResponse<Boolean> removeQuestionFromPaper(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getId() == null) {
            throw new RuntimeException("关联ID不能为空");
        }
        boolean result = paperQuestionService.removeQuestionFromPaper(deleteRequest.getId());
        return ResultUtils.success(result);
    }


    //检测爬虫
    private void crawlerDetect(long loginUserId) {
        // 调用多少次时告警
        final int WARN_COUNT = 10;
        // 超过多少次封号
        final int BAN_COUNT = 20;
        // 拼接访问 key
        String key = String.format("user:access:%s", loginUserId);
        // 一分钟内访问次数，180 秒过期
        long count = counterManager.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 180);
        // 是否封号
        if (count > BAN_COUNT) {
            // 踢下线
            StpUtil.kickout(loginUserId);
            // 封号
            User updateUser = new User();
            updateUser.setId(loginUserId);
            updateUser.setRole("ban");
            userService.updateById(updateUser);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问太频繁，已被封号");
        }
        // 是否告警
        if (count == WARN_COUNT) {
            // 可以改为向管理员发送邮件通知
            throw new BusinessException(110, "警告访问太频繁");
        }
    }

}