package com.jpkocommunity.domain.point.entity;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
// 포인트 엔티티
public class Point extends BaseEntity {
    // 포인트 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 보유 포인트 총액
    private Long point;

    // 실사용 가능 포인트 (거래 지연등으로 인한 거래 내역 반영 지연시)
    private Long real_point;
}
