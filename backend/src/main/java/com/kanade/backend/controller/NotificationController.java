package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.model.entity.SystemNotification;
import com.kanade.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Tag(name = "系统通知")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    @SaCheckLogin
    @Operation(summary = "获取未读通知列表")
    public BaseResponse<List<SystemNotification>> getUnread() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/all")
    @SaCheckLogin
    @Operation(summary = "获取所有通知列表")
    public BaseResponse<List<SystemNotification>> getAll() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(notificationService.getAllNotifications(userId));
    }

    @GetMapping("/count")
    @SaCheckLogin
    @Operation(summary = "获取未读通知数量")
    public BaseResponse<Integer> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/read/{id}")
    @SaCheckLogin
    @Operation(summary = "标记通知为已读")
    public BaseResponse<Boolean> markAsRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.markAsRead(id, userId);
        return ResultUtils.success(true);
    }
}
