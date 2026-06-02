package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.post.entity.PostImage;

public record PostImageResponse(
        Long id,
        String imageUrl,
        int displayOrder
) {
    public static PostImageResponse from(PostImage postImage) {
        return new PostImageResponse(
                postImage.getId(),
                postImage.getImageUrl(),
                postImage.getDisplayOrder()
        );
    }
}