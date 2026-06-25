package com.jpkocommunity.domain.notice.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "notices")
public class Notice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int viewCount;

    // 게시글 목록 상단 공지
    @Column(nullable = false)
    private boolean pinned;

    // 메인 상단 중요 공지
    @Column(nullable = false)
    private boolean featured;

    @Builder
    public Notice(User user, String title, String content, boolean pinned, boolean featured) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.featured = featured;
        this.viewCount = 0;
    }

    // ========== 비즈니스 메서드 ==========

    public void update(String title, String content, boolean pinned, boolean featured) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.featured = featured;
    }

    public void confirmContent(String content) {
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
