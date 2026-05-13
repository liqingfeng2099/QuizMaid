package com.kanade.backend.service.impl;

import com.kanade.backend.mapper.SystemNotificationMapper;
import com.kanade.backend.model.entity.SystemNotification;
import com.kanade.backend.service.NotificationService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification>
        implements NotificationService {

    @Override
    public void sendNotification(Long userId, String title, String content, Integer type, String link) {
        SystemNotification notif = new SystemNotification();
        notif.setUserId(userId);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setType(type);
        notif.setIsRead(0);
        notif.setLink(link);
        notif.setCreateTime(LocalDateTime.now());
        this.save(notif);
    }

    @Override
    public List<SystemNotification> getUnreadNotifications(Long userId) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId)
                .eq("isRead", 0)
                .orderBy("createTime", false);
        return this.list(qw);
    }

    @Override
    public List<SystemNotification> getAllNotifications(Long userId) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false)
                .limit(50);
        return this.list(qw);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        SystemNotification notif = new SystemNotification();
        notif.setId(notificationId);
        notif.setIsRead(1);
        this.updateById(notif);
    }

    @Override
    public int getUnreadCount(Long userId) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId)
                .eq("isRead", 0);
        return (int) this.count(qw);
    }
}
