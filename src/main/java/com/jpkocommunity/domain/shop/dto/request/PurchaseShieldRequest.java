package com.jpkocommunity.domain.shop.dto.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseShieldRequest(
        @NotNull(message = "사용할 상품을 선택해주세요.")
        Long itemId
) {}
