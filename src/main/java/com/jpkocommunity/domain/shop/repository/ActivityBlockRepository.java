package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.ActivityBlock;
import com.jpkocommunity.domain.shop.entity.BlockStatus;
import com.jpkocommunity.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ActivityBlockRepository extends JpaRepository<ActivityBlock, Long> {

    Optional<ActivityBlock> findByTargetAndStatus(User target, BlockStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ActivityBlock b where b.target = :target and b.status = 'ACTIVE'")
    Optional<ActivityBlock> findActiveByTargetForUpdate(@Param("target") User target);

    // JwtAuthenticationFilter가 매 요청마다 조회하는 hot path
    boolean existsByTarget_IdAndStatusAndExpiresAtAfter(Long targetId, BlockStatus status, LocalDateTime now);

    @Modifying
    @Query("update ActivityBlock b set b.status = 'EXPIRED' where b.status = 'ACTIVE' and b.expiresAt <= :now")
    int expireAllPast(@Param("now") LocalDateTime now);
}
