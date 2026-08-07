package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.RouletteShield;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RouletteShieldStatusResponse(
        boolean active,
        String itemName,
        LocalDateTime expiresAt
) {
    public static RouletteShieldStatusResponse inactive() {
        return RouletteShieldStatusResponse.builder().active(false).build();
    }

    public static RouletteShieldStatusResponse from(RouletteShield shield) {
        return RouletteShieldStatusResponse.builder()
                .active(true)
                .itemName(shield.getItem().getName())
                .expiresAt(shield.getExpiresAt())
                .build();
    }
}
