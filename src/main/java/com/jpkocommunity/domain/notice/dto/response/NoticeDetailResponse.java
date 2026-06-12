package com.jpkocommunity.domain.notice.dto.response;

import com.jpkocommunity.domain.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        String author,
        int viewCount,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getUser().getNickname(),
                notice.getViewCount(),
                notice.isPinned(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}