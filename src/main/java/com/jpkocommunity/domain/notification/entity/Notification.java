package com.jpkocommunity.domain.notification.entity;

import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    // POINT_STOLEN처럼 게시글과 무관한 알림은 null (룰렛으로 포인트를 뺏긴 경우 등)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false)
    private boolean isRead;

    // POINT_STOLEN 알림에서 뺏긴/뺏은 포인트 금액. 그 외 타입은 null
    private Long amount;

    @Builder
    public Notification(User receiver, User sender, NotificationType type, Post post, Comment comment, Long amount) {
        this.receiver = receiver;
        this.sender = sender;
        this.type = type;
        this.post = post;
        this.comment = comment;
        this.isRead = false;
        this.amount = amount;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
