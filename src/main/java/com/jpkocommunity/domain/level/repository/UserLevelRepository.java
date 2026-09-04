package com.jpkocommunity.domain.level.repository;

import com.jpkocommunity.domain.level.entity.UserLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {

    Optional<UserLevel> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ul from UserLevel ul where ul.user.id = :userId")
    Optional<UserLevel> findByUserIdForUpdate(@Param("userId") Long userId);
}
