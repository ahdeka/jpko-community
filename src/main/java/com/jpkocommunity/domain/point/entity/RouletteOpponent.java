package com.jpkocommunity.domain.point.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 유저가 고정해둔 룰렛 상대. 유저당 최대 1행 — 스핀 시 이 행이 있으면 랜덤 대신 이 상대와 겨룬다.
@Getter
@Entity
@Table(name = "roulette_opponents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RouletteOpponent extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "opponent_id", nullable = false)
    private Long opponentId;

    @Builder
    public RouletteOpponent(Long userId, Long opponentId) {
        this.userId = userId;
        this.opponentId = opponentId;
    }

    public void changeOpponent(Long opponentId) {
        this.opponentId = opponentId;
    }
}
