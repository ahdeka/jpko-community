package com.jpkocommunity.domain.shop.dto.request;

// 필드는 모두 선택 — 넘어온 값만 반영. 이미 OPEN인 경매 회차에는 소급 적용되지 않고 다음 회차부터 반영된다.
public record UpdateShopItemRequest(
        Long price,
        Long durationMinutes,
        Integer quantity,
        Long minPrice,
        Integer durationDays,
        Integer cadenceDays,
        Boolean active
) {}
