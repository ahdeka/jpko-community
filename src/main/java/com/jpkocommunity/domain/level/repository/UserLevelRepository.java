package com.jpkocommunity.domain.level.repository;

import com.jpkocommunity.domain.level.entity.UserLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {

    Optional<UserLevel> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ul from UserLevel ul where ul.user.id = :userId")
    Optional<UserLevel> findByUserIdForUpdate(@Param("userId") Long userId);

    // 목록 화면용 배치 조회 - 작성자 수 만큼 쿼리가 나가는 N+1을 IN 쿼리로 해결
    @Query("select ul.user.id as userId, ul.exp as exp from UserLevel ul where ul.user.id in :userIds")
    List<UserExp> findExpByUserIdIn(@Param("userIds") List<Long> userIds);
}
