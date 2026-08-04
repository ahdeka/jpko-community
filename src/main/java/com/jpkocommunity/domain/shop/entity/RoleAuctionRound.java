package com.jpkocommunity.domain.shop.entity;

import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 상품(shop_item, type=ROLE_AUCTION)당 OPEN 회차는 항상 최대 1개 (DB 부분 유니크 인덱스로도 보장)
@Getter
@Entity
@Table(name = "role_auction_rounds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleAuctionRound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private ShopItem shopItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole targetRole;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long minPrice;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionRoundStatus status;

    @Builder
    private RoleAuctionRound(ShopItem shopItem, UserRole targetRole, Integer quantity, Long minPrice,
                              LocalDateTime startAt, LocalDateTime endAt) {
        this.shopItem = shopItem;
        this.targetRole = targetRole;
        this.quantity = quantity;
        this.minPrice = minPrice;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = AuctionRoundStatus.OPEN;
    }

    // shopItem 설정값을 스냅샷으로 고정한 회차 생성 — 진행 중인 회차에는 이후 shopItem 변경이 소급 적용되지 않는다
    public static RoleAuctionRound open(ShopItem shopItem) {
        LocalDateTime now = LocalDateTime.now();
        return RoleAuctionRound.builder()
                .shopItem(shopItem)
                .targetRole(shopItem.getTargetRole())
                .quantity(shopItem.getQuantity())
                .minPrice(shopItem.getMinPrice())
                .startAt(now)
                .endAt(now.plusDays(shopItem.getDurationDays()))
                .build();
    }

    public void settle() {
        this.status = AuctionRoundStatus.SETTLED;
    }

    public boolean isOpenAndEnded() {
        return this.status == AuctionRoundStatus.OPEN && this.endAt.isBefore(LocalDateTime.now());
    }
}
