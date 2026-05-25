package com.tradev.domain.notification.dto;

import com.tradev.domain.notification.entity.Notification;
import com.tradev.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String message,
    String link,
    boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getMessage(),
            notification.getLink(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
