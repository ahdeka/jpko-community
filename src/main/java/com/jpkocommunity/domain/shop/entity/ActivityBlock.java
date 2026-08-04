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

// 유저당 ACTIVE row는 항상 최대 1개 (DB 부분 유니크 인덱스로도 보장)
@Getter
@Entity
@Table(name = "activity_blocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityBlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_user_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ShopItem item;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockStatus status;

    @Builder
    private ActivityBlock(User target, User blocker, ShopItem item, Long price, LocalDateTime expiresAt) {
        this.target = target;
        this.blocker = blocker;
        this.item = item;
        this.price = price;
        this.expiresAt = expiresAt;
        this.status = BlockStatus.ACTIVE;
    }

    public static ActivityBlock create(User target, User blocker, ShopItem item, Long price) {
        return ActivityBlock.builder()
                .target(target)
                .blocker(blocker)
                .item(item)
                .price(price)
                .expiresAt(LocalDateTime.now().plusMinutes(item.getDurationMinutes()))
                .build();
    }

    // ========== 비즈니스 메서드 ==========

    // 재차단: 기존 만료 시각에 새 아이템의 지속시간을 더해 연장 (요구사항: 기간 누적)
    public void extend(User newBlocker, ShopItem newItem, Long newPrice) {
        this.blocker = newBlocker;
        this.item = newItem;
        this.price = newPrice;
        this.expiresAt = this.expiresAt.plusMinutes(newItem.getDurationMinutes());
    }

    public void lift() {
        this.status = BlockStatus.LIFTED;
    }

    public void expire() {
        this.status = BlockStatus.EXPIRED;
    }

    public boolean isCurrentlyActive() {
        return this.status == BlockStatus.ACTIVE && this.expiresAt.isAfter(LocalDateTime.now());
    }
}
