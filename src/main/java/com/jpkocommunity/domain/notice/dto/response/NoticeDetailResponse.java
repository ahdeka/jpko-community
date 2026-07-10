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
        boolean featured,
        LocalDateTime createdAt
) {
    // viewCount는 벌크 UPDATE로 처리하기 때문에, 따로 인자로 받아서 설정
    public static NoticeDetailResponse from(Notice notice, int viewCount) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getUser().getDisplayNickname(),
                viewCount,
                notice.isPinned(),
                notice.isFeatured(),
                notice.getCreatedAt()
        );
    }
}