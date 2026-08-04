package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.RoleAuctionRound;
import com.jpkocommunity.domain.user.entity.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RoleAuctionRoundResponse(
        Long id,
        Long shopItemId,
        UserRole targetRole,
        Integer quantity,
        Long minPrice,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long topBidAmount,
        int activeBidCount
) {
    // activeBidsDesc: 회차의 ACTIVE 입찰을 금액 내림차순으로 정렬한 목록 (RoleBidRepository 조회 결과 그대로 전달)
    public static RoleAuctionRoundResponse of(RoleAuctionRound round, List<Long> activeBidAmountsDesc) {
        return RoleAuctionRoundResponse.builder()
                .id(round.getId())
                .shopItemId(round.getShopItem().getId())
                .targetRole(round.getTargetRole())
                .quantity(round.getQuantity())
                .minPrice(round.getMinPrice())
                .startAt(round.getStartAt())
                .endAt(round.getEndAt())
                .topBidAmount(activeBidAmountsDesc.isEmpty() ? null : activeBidAmountsDesc.get(0))
                .activeBidCount(activeBidAmountsDesc.size())
                .build();
    }
}
