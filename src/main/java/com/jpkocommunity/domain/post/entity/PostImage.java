package com.jpkocommunity.domain.post.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "post_images")
public class PostImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // S3에 저장된 파일 경로 (예: posts/1/uuid.jpg)
    @Column(nullable = false)
    private String s3Key;

    // 클라이언트에 노출되는 전체 URL (예: https://jpkocommunity-images.s3.ap-northeast-2.amazonaws.com/posts/1/uuid.jpg)
    @Column(nullable = false)
    private String imageUrl;

    // 업로드 순서 (게시글 내 이미지 정렬용)
    @Column(nullable = false)
    private int displayOrder;

    @Builder
    public PostImage(Post post, String s3Key, String imageUrl, int displayOrder) {
        this.post = post;
        this.s3Key = s3Key;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}