package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.RolePurchase;
import com.jpkocommunity.domain.user.entity.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RolePurchaseAdminResponse(
        Long userId,
        String nickname,
        UserRole targetRole,
        UserRole previousRole,
        LocalDateTime expiresAt
) {
    public static RolePurchaseAdminResponse from(RolePurchase purchase) {
        return RolePurchaseAdminResponse.builder()
                .userId(purchase.getUser().getId())
                .nickname(purchase.getUser().getDisplayNickname())
                .targetRole(purchase.getTargetRole())
                .previousRole(purchase.getPreviousRole())
                .expiresAt(purchase.getExpiresAt())
                .build();
    }
}
