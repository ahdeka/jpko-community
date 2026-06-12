package com.jpkocommunity.domain.notice.dto.response;

import com.jpkocommunity.domain.notice.entity.Notice;

import java.time.LocalDateTime;

// 공지 목록 / 게시판 상단 고정 노출 양쪽에서 사용
public record NoticeSummaryResponse(
        Long id,
        String title,
        String author,    // 관리자 닉네임
        int viewCount,
        boolean pinned,
        LocalDateTime createdAt
) {
    public static NoticeSummaryResponse from(Notice notice) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getUser().getNickname(),
                notice.getViewCount(),
                notice.isPinned(),
                notice.getCreatedAt()
        );
    }
}