package com.jpkocommunity.domain.notice.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int viewCount;

    // true면 모든 게시판 목록 상단에 고정 노출
    @Column(nullable = false)
    private boolean pinned;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<NoticeImage> images = new ArrayList<>();

    @Builder
    public Notice(User user, String title, String content, boolean pinned) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.viewCount = 0;
    }

    // ========== 비즈니스 메서드 ==========

    public void update(String title, String content, boolean pinned) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
