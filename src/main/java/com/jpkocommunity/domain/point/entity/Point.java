package com.jpkocommunity.domain.point.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    // 포인트 소유자.
    // null이면 시스템 계정 (포인트 발행)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long balance;

    @Builder
    public Point(User user, Long balance) {
        this.user = user;
        this.balance = balance;
    }

    public boolean isSystemAccount() {
        return user == null;
    }

    // 수신인
    public void credit(long amount) {
        this.balance += amount;
    }

    // 송신인
    // 시스템 계정 -> 마이너스 ㅇㅋ
    public void debit(long amount) {
        if (!isSystemAccount() && this.balance < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance -= amount;
    }
}