package com.jpkocommunity.domain.notice.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "notice_images")
public class NoticeImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    // S3에 저장된 파일 경로 (예: notices/1/uuid.jpg)
    @Column(nullable = false)
    private String s3Key;

    // 클라이언트에 노출되는 전체 URL
    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;

    @Builder
    public NoticeImage(Notice notice, String s3Key, String imageUrl, int displayOrder) {
        this.notice = notice;
        this.s3Key = s3Key;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}
