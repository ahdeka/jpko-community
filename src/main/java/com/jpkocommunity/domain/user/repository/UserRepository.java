package com.jpkocommunity.domain.user.repository;

import com.jpkocommunity.domain.auth.repository.UserAuthProjection;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByLoginId(String loginId);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByLoginId(String loginId);

    // 마이그레이션용 조회 쿼리 TODO 마이그레이션 끝나면 정리될 메서드
    @Query("SELECT u.status AS status, u.loginId AS loginId, u.emailVerified AS emailVerified " +
            "FROM User u WHERE u.id = :userId")
    Optional<UserAuthProjection> findAuthProjectionById(Long userId);

    // Admin 기능
    Page<User> findByNicknameContainingOrEmailContaining(String nickname, String email, Pageable pageable);

    // 일일 포인트 룰렛 피해자 선정용: 본인을 제외한 활성 유저 중 무작위 1명.
    // ROOT는 정지/권한변경 등 다른 관리 기능에서도 보호 대상이라 룰렛 피해자에서도 제외한다.
    // ORDER BY RANDOM()은 대상 row 전체에 난수를 매겨 정렬해야 해서 유저가 많아지면 느려짐 ->
    // COUNT로 대상 수를 센 뒤 그 범위 안에서 OFFSET만 무작위로 골라 정렬 없이 1건만 읽는다.
    // src/main/java/com/jpkocommunity/domain/user/repository/UserRepository.java

    @Query(value = """
            SELECT u.id FROM users u
            JOIN points p ON p.user_id = u.id
            WHERE u.status = 'ACTIVE' AND u.role <> 'ROOT' AND u.id <> :excludeUserId
              AND u.email_verified = true
              AND p.balance >= :betAmount
            OFFSET floor(random() * (
                SELECT COUNT(*) FROM users u2
                JOIN points p2 ON p2.user_id = u2.id
                WHERE u2.status = 'ACTIVE' AND u2.role <> 'ROOT' AND u2.id <> :excludeUserId
                  AND u2.email_verified = true
                  AND p2.balance >= :betAmount
            ))::bigint
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findRandomActiveUserIdExcluding(
            @Param("excludeUserId") Long excludeUserId, @Param("betAmount") long betAmount);

    // 기한이 지난 정지를 자동으로 해제 (경매로 획득한 ADMIN 권한이 부여한 기한부 정지 포함)
    @Modifying
    @Query("update User u set u.status = 'ACTIVE', u.suspendedUntil = null " +
            "where u.status = 'SUSPENDED' and u.suspendedUntil is not null and u.suspendedUntil <= :now")
    int reactivateAllExpiredSuspensions(@Param("now") LocalDateTime now);
}