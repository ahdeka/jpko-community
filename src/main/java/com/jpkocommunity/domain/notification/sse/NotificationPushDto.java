package com.jpkocommunity.domain.notification.sse;

import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;

import java.time.LocalDateTime;

// 알림 푸시용 DTO
public record NotificationPushDto(
        Long id,
        NotificationType type,
        boolean isAnonymous,
        Long senderId,        // 익명이면 null
        Long postId,
        Long commentId,
        LocalDateTime createdAt
) {
    public static NotificationPushDto of(Notification saved, NotificationEvent event) {
        return new NotificationPushDto(
                saved.getId(),
                event.type(),
                event.isAnonymous(),
                event.isAnonymous() ? null : event.senderId(),
                event.postId(),
                event.commentId(),
                saved.getCreatedAt()
        );
    }
}