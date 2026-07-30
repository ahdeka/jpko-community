package com.jpkocommunity.domain.user.repository;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByIdAndStatus(Long id, UserStatus status);

    // Admin 기능
    Page<User> findByNicknameContainingOrEmailContaining(String nickname, String email, Pageable pageable);

    // 일일 포인트 룰렛 피해자 선정용: 본인을 제외한 활성 유저 중 무작위 1명
    @Query(value = "SELECT id FROM users WHERE status = 'ACTIVE' AND id <> :excludeUserId ORDER BY RANDOM() LIMIT 1",
            nativeQuery = true)
    Optional<Long> findRandomActiveUserIdExcluding(@Param("excludeUserId") Long excludeUserId);
}