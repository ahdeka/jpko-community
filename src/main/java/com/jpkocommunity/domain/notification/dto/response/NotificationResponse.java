package com.jpkocommunity.domain.notification.dto.response;

import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        boolean isAnonymous,
        String senderName,     // 익명이면 null
        Long postId,
        String postTitle,
        Long commentId,
        boolean isRead,
        LocalDateTime createdAt,
        Long amount             // POINT_STOLEN 외 타입은 null
) {
    public static NotificationResponse from(Notification n) {
        boolean anonymous = isAnonymousComment(n);
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                anonymous,
                anonymous ? null : n.getSender().getDisplayNickname(),
                n.getPost() != null ? n.getPost().getId() : null,
                n.getPost() != null ? n.getPost().getTitle() : null,
                n.getComment() != null ? n.getComment().getId() : null,
                n.isRead(),
                n.getCreatedAt(),
                n.getAmount()
        );
    }

    private static boolean isAnonymousComment(Notification n) {
        Comment comment = n.getComment();
        return comment != null && comment.isAnonymous();
    }
}
