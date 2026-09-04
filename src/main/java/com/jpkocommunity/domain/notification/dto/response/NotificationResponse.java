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
        String commentContent, // COMMENT_REWARD처럼 어떤 댓글로 받았는지 보여줘야 하는 타입 전용. 그 외 타입은 null
        boolean isRead,
        LocalDateTime createdAt,
        Long amount             // 타입별 의미가 다름
) {
    public static NotificationResponse from(Notification n) {
        boolean anonymous = isAnonymousComment(n);
        // 시스템이 발행한 알림(포인트 지급)은 sender가 없음
        String senderName = (anonymous || n.getSender() == null) ? null : n.getSender().getDisplayNickname();
        Comment comment = n.getComment();
        boolean commentDeleted = comment != null && comment.isDeleted();
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                anonymous,
                senderName,
                n.getPost() != null ? n.getPost().getId() : null,
                n.getPost() != null ? n.getPost().getTitle() : null,
                comment != null ? comment.getId() : null,
                commentDeleted ? null : (comment != null ? comment.getContent() : null),
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
