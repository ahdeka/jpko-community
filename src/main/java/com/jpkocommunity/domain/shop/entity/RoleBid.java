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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회차(round)당 유저 입찰은 1 row (DB uk_role_bids_round_bidder). 재입찰은 금액을 올리는 것만 허용.
@Getter
@Entity
@Table(name = "role_bids")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleBid extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private RoleAuctionRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_user_id", nullable = false)
    private User bidder;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    // 같은 유저의 "입찰 인상" 요청 두 건이 거의 동시에 들어와도 낙관적 락으로 하나는 충돌 처리되게 한다
    // - 없으면 두 요청 다 포인트는 정상 차감되는데 amount는 나중에 커밋된 쪽 값으로 덮어써져
    // 실제 지불액보다 낮게 기록될 수 있다(이후 유찰 시 환불 부족).
    @Version
    private Long version;

    @Builder
    private RoleBid(RoleAuctionRound round, User bidder, Long amount) {
        this.round = round;
        this.bidder = bidder;
        this.amount = amount;
        this.status = BidStatus.ACTIVE;
    }

    public static RoleBid place(RoleAuctionRound round, User bidder, Long amount) {
        return RoleBid.builder().round(round).bidder(bidder).amount(amount).build();
    }

    public void raiseTo(Long newAmount) {
        this.amount = newAmount;
    }

    public void win() {
        this.status = BidStatus.WON;
    }

    public void lose() {
        this.status = BidStatus.LOST;
    }
}
