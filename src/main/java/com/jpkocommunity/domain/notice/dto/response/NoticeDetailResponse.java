package com.jpkocommunity.domain.notice.dto.response;

import com.jpkocommunity.domain.notice.entity.Notice;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        String author,
        int viewCount,
        boolean pinned,
        List<NoticeImageResponse> images,
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
                notice.getImages().stream()
                        .map(NoticeImageResponse::from)
                        .toList(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}