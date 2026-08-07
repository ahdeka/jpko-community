package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.RouletteShield;
import com.jpkocommunity.domain.shop.entity.RouletteShieldStatus;
import com.jpkocommunity.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RouletteShieldRepository extends JpaRepository<RouletteShield, Long> {

    Optional<RouletteShield> findByUserAndStatus(User user, RouletteShieldStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RouletteShield s where s.user = :user and s.status = 'ACTIVE'")
    Optional<RouletteShield> findActiveByUserForUpdate(@Param("user") User user);

    @Query("select s from RouletteShield s where s.status = 'ACTIVE' and s.expiresAt <= :now")
    List<RouletteShield> findAllExpired(@Param("now") LocalDateTime now);
}
