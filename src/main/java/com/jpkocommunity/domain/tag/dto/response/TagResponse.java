package com.jpkocommunity.domain.tag.dto.response;

import com.jpkocommunity.domain.tag.entity.Tag;

public record TagResponse(
        Long id,
        String name,
        boolean fixed
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.isFixed()
        );
    }
}
