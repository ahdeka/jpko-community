package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.ShopItem;
import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.user.entity.UserRole;
import lombok.Builder;

@Builder
public record ShopItemResponse(
        Long id,
        ShopItemType type,
        String name,
        Long price,
        Long durationMinutes,
        UserRole targetRole,
        Integer quantity,
        Long minPrice,
        Integer durationDays,
        Integer cadenceDays,
        boolean active
) {
    public static ShopItemResponse from(ShopItem item) {
        return ShopItemResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .name(item.getName())
                .price(item.getPrice())
                .durationMinutes(item.getDurationMinutes())
                .targetRole(item.getTargetRole())
                .quantity(item.getQuantity())
                .minPrice(item.getMinPrice())
                .durationDays(item.getDurationDays())
                .cadenceDays(item.getCadenceDays())
                .active(item.isActive())
                .build();
    }
}
