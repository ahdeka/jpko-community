package com.jpkocommunity.domain.notification.event;

import com.jpkocommunity.domain.notification.entity.NotificationType;

public record NotificationEvent(
        Long receiverId,
        Long senderId,
        NotificationType type,
        Long postId,
        Long commentId,
        boolean isAnonymous,
        Long amount // 타입별 의미가 다름
) {}