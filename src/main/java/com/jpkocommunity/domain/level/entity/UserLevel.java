package com.jpkocommunity.domain.level.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "user_levels")
@NoArgsConstructor(access = PROTECTED)
public class UserLevel extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private long exp;

    @Column(nullable = false)
    private long expEarnedToday;

    private LocalDate expEarnedDate;

    @Builder
    public UserLevel(User user) {
        this.user = user;
        this.exp = 0L;
        this.expEarnedToday = 0L;
    }

    // ========== 비즈니스 메서드 ==========

    public long gainExp(long expAmount, long dailyLimit, LocalDate today) {
        if (expAmount <= 0) return 0;

        if (!today.equals(this.expEarnedDate)) {
            this.expEarnedDate = today;
            this.expEarnedToday = 0L;
        }

        long grantable = Math.clamp(dailyLimit - this.expEarnedToday, 0, expAmount);
        if (grantable <= 0) return 0;

        this.exp += grantable;
        this.expEarnedToday += grantable;
        return grantable;
    }
}
