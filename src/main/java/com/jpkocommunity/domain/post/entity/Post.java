package com.jpkocommunity.domain.post.entity;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "posts")
public class Post extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
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

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

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

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // 익명 게시글에서 ㅇㅇ(118.235) 형태로 표시
    public String getMaskedIp() {
        if (ipAddress == null || ipAddress.isBlank()) return "알 수 없음";

        // IPv6 로컬 주소 처리
        if (ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("::1")) {
            return "127.0";
        }

        String[] parts = ipAddress.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return "알 수 없음";
    }
}