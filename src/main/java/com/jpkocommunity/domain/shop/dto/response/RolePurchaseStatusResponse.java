package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.RolePurchase;
import com.jpkocommunity.domain.user.entity.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RolePurchaseStatusResponse(
        boolean active,
        UserRole targetRole,
        LocalDateTime expiresAt
) {
    public static RolePurchaseStatusResponse none() {
        return RolePurchaseStatusResponse.builder().active(false).build();
    }

    public static RolePurchaseStatusResponse from(RolePurchase purchase) {
        return RolePurchaseStatusResponse.builder()
                .active(true)
                .targetRole(purchase.getTargetRole())
                .expiresAt(purchase.getExpiresAt())
                .build();
    }
}
