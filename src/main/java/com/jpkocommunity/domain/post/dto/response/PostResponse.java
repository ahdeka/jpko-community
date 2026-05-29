package com.jpkocommunity.domain.post.dto.response;

public record PostResponse(Long id) {

    public static PostResponse from(Long id) {
        return new PostResponse(id);
    }
}