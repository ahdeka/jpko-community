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

    // 상점에서 구매한 차단이면 그 상품을 가리킨다. null이면 ADMIN이 관리자 페이지에서 건 정지가
    // 이 차단으로 대체된 것(구매가 아니므로 연결할 상품이 없음) — BlockStatusResponse 등에서
    // 이 null 여부로 "구매한 차단"과 "관리자 정지" 표시를 구분한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
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

    // ADMIN이 관리자 페이지에서 유저를 정지시키면(ROOT의 진짜 정지와 달리) User.status를 건드리지
    // 않고 이 차단으로 대체한다 - item 없이 만료 시각을 직접 받는다.
    public static ActivityBlock createByAdminSuspend(User target, User admin, Long price, LocalDateTime expiresAt) {
        return ActivityBlock.builder()
                .target(target)
                .blocker(admin)
                .item(null)
                .price(price)
                .expiresAt(expiresAt)
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

    // ADMIN이 이미 활성 상태인(구매된 것이든 이전 관리자 정지든) 차단을 다시 관리자 정지로 덮어쓴다.
    // 정지는 기존에도 누적이 아니라 항상 새 기한으로 덮어썼으므로(user.suspend) extend()처럼 기간을
    // 더하지 않고 그대로 대체한다.
    public void reassignAsAdminSuspension(User admin, Long price, LocalDateTime expiresAt) {
        this.blocker = admin;
        this.item = null;
        this.price = price;
        this.expiresAt = expiresAt;
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
