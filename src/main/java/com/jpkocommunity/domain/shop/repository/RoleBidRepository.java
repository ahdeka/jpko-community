package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.BidStatus;
import com.jpkocommunity.domain.shop.entity.RoleAuctionRound;
import com.jpkocommunity.domain.shop.entity.RoleBid;
import com.jpkocommunity.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleBidRepository extends JpaRepository<RoleBid, Long> {

    Optional<RoleBid> findByRoundAndBidder(RoleAuctionRound round, User bidder);

    // 정산 시 상위 quantity명 선정: 금액 내림차순, 동점은 먼저 입찰한 순
    List<RoleBid> findByRoundAndStatusOrderByAmountDescCreatedAtAsc(RoleAuctionRound round, BidStatus status);
}
