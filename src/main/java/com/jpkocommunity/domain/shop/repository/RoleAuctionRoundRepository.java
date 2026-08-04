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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoleAuctionRound r where r.status = 'OPEN' and r.endAt <= :now")
    List<RoleAuctionRound> findAllOpenAndEndedForUpdate(@Param("now") LocalDateTime now);

    Optional<RoleAuctionRound> findByIdAndStatus(Long id, AuctionRoundStatus status);

    List<RoleAuctionRound> findByStatus(AuctionRoundStatus status);
}
