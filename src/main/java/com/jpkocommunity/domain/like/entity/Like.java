package com.jpkocommunity.domain.like.entity;

import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "likes",
        // 한 유저가 같은 게시글에 중복 좋아요/비추천 방지
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // LIKE / DISLIKE 구분
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LikeType type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Like(Post post, User user, LikeType type) {
        this.post = post;
        this.user = user;
        this.type = type;
    }

    // 같은 버튼 재클릭 시 타입 변경 (LIKE → DISLIKE, DISLIKE → LIKE)
    public void changeType(LikeType type) {
        this.type = type;
    }
}