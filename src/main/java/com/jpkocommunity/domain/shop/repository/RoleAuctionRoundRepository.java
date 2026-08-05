package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.AuctionRoundStatus;
import com.jpkocommunity.domain.shop.entity.RoleAuctionRound;
import com.jpkocommunity.domain.shop.entity.ShopItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoleAuctionRoundRepository extends JpaRepository<RoleAuctionRound, Long> {

    Optional<RoleAuctionRound> findByShopItemAndStatus(ShopItem shopItem, AuctionRoundStatus status);

    // 정산 대상 후보 id만 락 없이 조회한다 - 회차별로 독립된 트랜잭션에서 각각 락을 걸고 정산해야
    // 한 회차의 실패가 나머지 회차를 막지 않는다 (RoleAuctionService.settleExpiredRounds 참고).
    @Query("select r.id from RoleAuctionRound r where r.status = 'OPEN' and r.endAt <= :now")
    List<Long> findAllOpenAndEndedIds(@Param("now") LocalDateTime now);

    Optional<RoleAuctionRound> findByIdAndStatus(Long id, AuctionRoundStatus status);

    // 회차 하나만 락을 걸고 정산한다 (settleRoundSafely 전용) - 조기종료(updateRoundEndAt)와도 공유.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoleAuctionRound r where r.id = :id and r.status = 'OPEN'")
    Optional<RoleAuctionRound> findByIdAndStatusForUpdate(@Param("id") Long id);

    Optional<RoleAuctionRound> findTopByShopItemAndStatusOrderByEndAtDesc(ShopItem shopItem, AuctionRoundStatus status);

    List<RoleAuctionRound> findByStatus(AuctionRoundStatus status);
}
