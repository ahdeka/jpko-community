package com.jpkocommunity.domain.shop.entity;

import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.global.entity.BaseEntity;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상점 상품 카탈로그. 가격/수량 조정은 ROOT 전용 (AdminShopItemController에서 강제)
@Getter
@Entity
@Table(name = "shop_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopItem extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopItemType type;

    @Column(nullable = false, length = 100)
    private String name;

    // BLOCK 전용
    private Long price;
    private Long durationMinutes;

    // ROLE_AUCTION 전용
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole targetRole;
    private Integer quantity;
    private Long minPrice;
    private Integer durationDays;
    private Integer cadenceDays;

    @Column(nullable = false)
    private boolean active;

    @Builder
    private ShopItem(ShopItemType type, String name, Long price, Long durationMinutes,
                      UserRole targetRole, Integer quantity, Long minPrice,
                      Integer durationDays, Integer cadenceDays) {
        this.type = type;
        this.name = name;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.targetRole = targetRole;
        this.quantity = quantity;
        this.minPrice = minPrice;
        this.durationDays = durationDays;
        this.cadenceDays = cadenceDays;
        this.active = true;
    }

    public static ShopItem createBlockItem(String name, Long price, Long durationMinutes) {
        return ShopItem.builder()
                .type(ShopItemType.BLOCK)
                .name(name)
                .price(price)
                .durationMinutes(durationMinutes)
                .build();
    }

    public static ShopItem createRoleAuctionItem(String name, UserRole targetRole, Integer quantity,
                                                  Long minPrice, Integer durationDays, Integer cadenceDays) {
        if (targetRole == UserRole.ROOT) {
            throw new CustomException(ErrorCode.CANNOT_AUCTION_ROOT_ROLE);
        }
        return ShopItem.builder()
                .type(ShopItemType.ROLE_AUCTION)
                .name(name)
                .targetRole(targetRole)
                .quantity(quantity)
                .minPrice(minPrice)
                .durationDays(durationDays)
                .cadenceDays(cadenceDays)
                .build();
    }

    // ========== 비즈니스 메서드 ==========

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateBlockPricing(Long price, Long durationMinutes) {
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    public void updateRoleAuctionConfig(Integer quantity, Long minPrice, Integer durationDays, Integer cadenceDays) {
        this.quantity = quantity;
        this.minPrice = minPrice;
        this.durationDays = durationDays;
        this.cadenceDays = cadenceDays;
    }
}
