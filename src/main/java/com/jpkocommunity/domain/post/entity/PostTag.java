package com.jpkocommunity.domain.post.entity;

import com.jpkocommunity.domain.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "post_tags",
        // 같은 게시글에 같은 태그가 중복으로 붙지 않도록 복합 unique 제약
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"})
)
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Post 삭제 시 PostTag도 함께 삭제되도록 Post에서 cascade 설정했으므로
    // 여기서는 단순 참조만
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    public PostTag(Post post, Tag tag) {
        this.post = post;
        this.tag = tag;
    }
}