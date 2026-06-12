package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.notice.dto.response.NoticeSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public record PostListResponse(
        List<NoticeSummaryResponse> pinnedNotices,
        Page<PostSummaryResponse> posts
) {
    public static PostListResponse of(
            List<NoticeSummaryResponse> pinnedNotices,
            Page<PostSummaryResponse> posts
    ) {
        return new PostListResponse(pinnedNotices, posts);
    }
}