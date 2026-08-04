package com.jpkocommunity.domain.shop.entity;

import com.jpkocommunity.domain.user.entity.User;
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

// 유저당 ACTIVE row는 항상 최대 1개 (DB 부분 유니크 인덱스로도 보장).
// previousRole은 최초 낙찰 시점에만 스냅샷 — 연속 낙찰(연장)로는 덮어쓰지 않는다.
@Getter
@Entity
@Table(name = "role_purchases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RolePurchase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole targetRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole previousRole;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolePurchaseStatus status;

    @Builder
    private RolePurchase(User user, UserRole targetRole, UserRole previousRole, LocalDateTime expiresAt) {
        this.user = user;
        this.targetRole = targetRole;
        this.previousRole = previousRole;
        this.expiresAt = expiresAt;
        this.status = RolePurchaseStatus.ACTIVE;
    }

    public static RolePurchase create(User user, UserRole targetRole, UserRole previousRole, LocalDateTime expiresAt) {
        return RolePurchase.builder()
                .user(user)
                .targetRole(targetRole)
                .previousRole(previousRole)
                .expiresAt(expiresAt)
                .build();
    }

    public void extendByDays(int days) {
        this.expiresAt = this.expiresAt.plusDays(days);
    }

    public void expire() {
        this.status = RolePurchaseStatus.EXPIRED;
    }
}
