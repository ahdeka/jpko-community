package com.jpkocommunity.domain.shop.dto.request;

import jakarta.validation.constraints.Positive;

// 필드는 모두 선택 — 넘어온 값만 반영. 이미 OPEN인 경매 회차에는 소급 적용되지 않고 다음 회차부터 반영된다.
public record UpdateShopItemRequest(
        @Positive(message = "가격은 0보다 커야 합니다.")
        Long price,
        @Positive(message = "지속 시간은 0보다 커야 합니다.")
        Long durationMinutes,
        @Positive(message = "수량은 0보다 커야 합니다.")
        Integer quantity,
        @Positive(message = "최저가는 0보다 커야 합니다.")
        Long minPrice,
        @Positive(message = "지속 일수는 0보다 커야 합니다.")
        Integer durationDays,
        @Positive(message = "개설 간격은 0보다 커야 합니다.")
        Integer cadenceDays,
        Boolean active
) {}
