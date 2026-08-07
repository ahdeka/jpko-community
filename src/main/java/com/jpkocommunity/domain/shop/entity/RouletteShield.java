package com.jpkocommunity.domain.shop.entity;

import com.jpkocommunity.domain.user.entity.User;
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

// 유저당 ACTIVE row는 항상 최대 1개 (DB 부분 유니크 인덱스로도 보장).
// previousRouletteStatus는 최초 구매 시점에만 스냅샷 — 연속 구매(연장)로는 덮어쓰지 않는다
// (role_purchases.previous_role과 동일한 패턴).
@Getter
@Entity
@Table(name = "roulette_shields")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouletteShield extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ShopItem item;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private boolean previousRouletteStatus;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouletteShieldStatus status;

    @Builder
    private RouletteShield(User user, ShopItem item, Long price, boolean previousRouletteStatus, LocalDateTime expiresAt) {
        this.user = user;
        this.item = item;
        this.price = price;
        this.previousRouletteStatus = previousRouletteStatus;
        this.expiresAt = expiresAt;
        this.status = RouletteShieldStatus.ACTIVE;
    }

    public static RouletteShield create(User user, ShopItem item, Long price, boolean previousRouletteStatus) {
        return RouletteShield.builder()
                .user(user)
                .item(item)
                .price(price)
                .previousRouletteStatus(previousRouletteStatus)
                .expiresAt(LocalDateTime.now().plusMinutes(item.getDurationMinutes()))
                .build();
    }

    // ========== 비즈니스 메서드 ==========

    // 재구매: 기존 만료 시각에 새 아이템의 지속시간을 더해 연장 (activity_blocks.extend와 동일한 방식).
    // previousRouletteStatus는 최초 구매 때 이미 스냅샷했으므로 여기서 다시 건드리지 않는다.
    public void extend(ShopItem newItem, Long newPrice) {
        this.item = newItem;
        this.price = newPrice;
        this.expiresAt = this.expiresAt.plusMinutes(newItem.getDurationMinutes());
    }

    public void expire() {
        this.status = RouletteShieldStatus.EXPIRED;
    }

    public boolean isCurrentlyActive() {
        return this.status == RouletteShieldStatus.ACTIVE && this.expiresAt.isAfter(LocalDateTime.now());
    }
}
