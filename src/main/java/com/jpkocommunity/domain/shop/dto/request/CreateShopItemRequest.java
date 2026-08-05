package com.jpkocommunity.domain.shop.dto.request;

import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// type=BLOCK: price/durationMinutes 필수
// type=ROLE_AUCTION: targetRole/quantity/minPrice/durationDays/cadenceDays 필수
// (타입별 조건부 필수라 @NotNull은 서비스단에서 확인하고, 여기서는 값이 있을 때 양수인지만 검증한다)
public record CreateShopItemRequest(
        @NotNull(message = "상품 종류를 선택해주세요.")
        ShopItemType type,

        @NotBlank(message = "상품명을 입력해주세요.")
        String name,

        @Positive(message = "가격은 0보다 커야 합니다.")
        Long price,
        @Positive(message = "지속 시간은 0보다 커야 합니다.")
        Long durationMinutes,

        UserRole targetRole,
        @Positive(message = "수량은 0보다 커야 합니다.")
        Integer quantity,
        @Positive(message = "최저가는 0보다 커야 합니다.")
        Long minPrice,
        @Positive(message = "지속 일수는 0보다 커야 합니다.")
        Integer durationDays,
        @Positive(message = "개설 간격은 0보다 커야 합니다.")
        Integer cadenceDays
) {}
