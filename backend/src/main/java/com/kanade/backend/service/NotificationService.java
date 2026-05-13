package com.kanade.backend.service;

import com.kanade.backend.model.entity.SystemNotification;
import com.mybatisflex.core.service.IService;
import java.util.List;

public interface NotificationService extends IService<SystemNotification> {

    void sendNotification(Long userId, String title, String content, Integer type, String link);

    List<SystemNotification> getUnreadNotifications(Long userId);

    List<SystemNotification> getAllNotifications(Long userId);

    void markAsRead(Long notificationId, Long userId);

    int getUnreadCount(Long userId);
}
