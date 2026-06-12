package com.jpkocommunity.domain.notice.dto.response;

import com.jpkocommunity.domain.notice.entity.NoticeImage;

public record NoticeImageResponse(
        Long id,
        String imageUrl,
        int displayOrder
) {
    public static NoticeImageResponse from(NoticeImage noticeImage) {
        return new NoticeImageResponse(
                noticeImage.getId(),
                noticeImage.getImageUrl(),
                noticeImage.getDisplayOrder()
        );
    }
}