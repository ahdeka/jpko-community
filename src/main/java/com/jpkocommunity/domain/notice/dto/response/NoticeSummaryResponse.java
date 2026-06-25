package com.jpkocommunity.domain.notice.dto.response;

import com.jpkocommunity.domain.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeSummaryResponse(
        Long id,
        String title,
        int viewCount,
        boolean pinned,
        boolean featured,
        LocalDateTime createdAt
) {
    public static NoticeSummaryResponse from(Notice notice) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getViewCount(),
                notice.isPinned(),
                notice.isFeatured(),
                notice.getCreatedAt()
        );
    }
}