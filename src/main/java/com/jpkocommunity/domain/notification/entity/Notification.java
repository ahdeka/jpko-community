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

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false)
    private boolean isRead;

    @Builder
    public Notification(User receiver, User sender, NotificationType type, Post post, Comment comment) {
        this.receiver = receiver;
        this.sender = sender;
        this.type = type;
        this.post = post;
        this.comment = comment;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
