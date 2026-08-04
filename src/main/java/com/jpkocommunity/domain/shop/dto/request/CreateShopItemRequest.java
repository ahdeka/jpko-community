package com.jpkocommunity.domain.shop.dto.request;

import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// type=BLOCK: price/durationMinutes 필수
// type=ROLE_AUCTION: targetRole/quantity/minPrice/durationDays/cadenceDays 필수
public record CreateShopItemRequest(
        @NotNull(message = "상품 종류를 선택해주세요.")
        ShopItemType type,

        @NotBlank(message = "상품명을 입력해주세요.")
        String name,

        Long price,
        Long durationMinutes,

        UserRole targetRole,
        Integer quantity,
        Long minPrice,
        Integer durationDays,
        Integer cadenceDays
) {}
