package com.jpkocommunity.domain.post.entity;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.comment.entity.CommentTargetType;
import com.jpkocommunity.domain.comment.entity.Commentable;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import com.jpkocommunity.global.util.IpUtils;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "posts")
public class Post extends BaseEntity implements Commentable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 선택적 익명: true면 익명 표시, false면 닉네임 표시
    @Column(nullable = false)
    private boolean anonymous;

    @Column(nullable = false)
    private int viewCount;

    // 작성자 IP - 익명 표시 및 어뷰징 대응용
    // 프론트에는 끝 두 자리만 노출, 전체 IP는 운영자만 확인
    @Column(length = 45)
    private String ipAddress;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public Post(User user, Category category, String title, String content,
                boolean anonymous, String ipAddress) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.anonymous = anonymous;
        this.ipAddress = ipAddress;
        this.viewCount = 0;
    }

    // ========== 비즈니스 메서드 ==========

    public void update(Category category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void confirmContent(String content) {
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // 익명 게시글에서 ㅇㅇ(118.235) 형태로 표시
    public String getMaskedIp() {
        return IpUtils.mask(ipAddress);
    }

    @Override
    public User getAuthor() {
        return user;
    }

    @Override
    public boolean isCommentActive() {
        return !isDeleted();
    }

    @Override
    public CommentTargetType getTargetType() {
        return CommentTargetType.POST;
    }
}