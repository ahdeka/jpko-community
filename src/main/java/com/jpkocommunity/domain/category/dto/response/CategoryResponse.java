package com.jpkocommunity.domain.category.dto.response;

import com.jpkocommunity.domain.category.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Integer displayOrder
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDisplayOrder()
        );
    }
}
