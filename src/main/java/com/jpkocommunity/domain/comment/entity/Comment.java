// src/main/java/com/jpkocommunity/domain/comment/entity/Comment.java
package com.jpkocommunity.domain.comment.entity;

import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "comments")
public class Comment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CommentTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // null → 일반 댓글 / not null → 대댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 대댓글 목록 - cascade 없음 (부모 삭제해도 대댓글은 "삭제된 댓글" 표시 유지)
    @OneToMany(mappedBy = "parent")
    @OrderBy("createdAt ASC")
    @BatchSize(size = 100)
    private List<Comment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(length = 45)
    private String ipAddress;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public Comment(CommentTargetType targetType, Long targetId, User user, Comment parent,
                   String content, String imageUrl, boolean anonymous, String ipAddress) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.user = user;
        this.parent = parent;
        this.content = content;
        this.imageUrl = imageUrl;
        this.anonymous = anonymous;
        this.ipAddress = ipAddress;
    }

    // ========== 비즈니스 메서드 ==========

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isSameTarget(CommentTargetType type, Long id) {
        return this.targetType == type && this.targetId.equals(id);
    }

    // Post와 동일한 마스킹 로직
    public String getMaskedIp() {
        if (ipAddress == null || ipAddress.isBlank()) return "알 수 없음";

        if (ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("::1")) return "127.0";
        String[] parts = ipAddress.split("\\.");
        if (parts.length >= 2) return parts[0] + "." + parts[1];
        return "알 수 없음";
    }
}