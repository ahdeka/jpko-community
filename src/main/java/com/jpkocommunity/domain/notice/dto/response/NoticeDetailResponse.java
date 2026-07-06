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
    // viewCount는 엔티티(notice.getViewCount())가 아니라 호출부에서 넘겨받는다.
    // 조회수 증가를 벌크 UPDATE로 처리하면 로드된 엔티티에는 +1이 반영되지 않으므로,
    // 표시용 조회수를 명시적으로 전달받아야 정확한 값이 응답된다.
    public static NoticeDetailResponse from(Notice notice, int viewCount) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getUser().getNickname(),
                viewCount,
                notice.isPinned(),
                notice.isFeatured(),
                notice.getCreatedAt()
        );
    }
}