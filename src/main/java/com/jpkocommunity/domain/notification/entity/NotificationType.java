package com.jpkocommunity.domain.notification.entity;

public enum NotificationType {
    COMMENT,        // 내 게시글에 댓글 달림
    REPLY,          // 내 댓글에 대댓글 달림
    LIKE,           // 내 게시글에 좋아요 눌림
    CONTENT_REMOVED // 관리자가 게시글을 삭제함
}
