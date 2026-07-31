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

    // 일일 포인트 룰렛 피해자 선정용: 본인을 제외한 활성 유저 중 무작위 1명.
    // ROOT는 정지/권한변경 등 다른 관리 기능에서도 보호 대상이라 룰렛 피해자에서도 제외한다.
    // ORDER BY RANDOM()은 대상 row 전체에 난수를 매겨 정렬해야 해서 유저가 많아지면 느려짐 ->
    // COUNT로 대상 수를 센 뒤 그 범위 안에서 OFFSET만 무작위로 골라 정렬 없이 1건만 읽는다.
    @Query(value = """
            SELECT id FROM users
            WHERE status = 'ACTIVE' AND role <> 'ROOT' AND id <> :excludeUserId
            OFFSET floor(random() * (
                SELECT COUNT(*) FROM users
                WHERE status = 'ACTIVE' AND role <> 'ROOT' AND id <> :excludeUserId
            ))::bigint
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findRandomActiveUserIdExcluding(@Param("excludeUserId") Long excludeUserId);
}