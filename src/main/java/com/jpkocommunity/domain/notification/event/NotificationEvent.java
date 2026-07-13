package com.jpkocommunity.domain.notification.event;

import com.jpkocommunity.domain.notification.entity.NotificationType;

public record NotificationEvent(
        Long receiverId,
        Long senderId,
        NotificationType type,
        Long postId,
        Long commentId,
        boolean isAnonymous
) {}