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

    public void cancel() {
        this.status = BidStatus.CANCELLED;
    }
}
