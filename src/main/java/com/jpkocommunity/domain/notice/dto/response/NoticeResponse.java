package com.jpkocommunity.domain.notice.dto.response;

public record NoticeResponse(Long id) {

    public static NoticeResponse from(Long id) {
        return new NoticeResponse(id);
    }
}