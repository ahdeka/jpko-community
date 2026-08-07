package com.jpkocommunity.domain.user.repository;

import com.jpkocommunity.domain.auth.repository.UserAuthProjection;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByLoginId(String loginId);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByLoginId(String loginId);

    // 매 요청마다 JwtAuthenticationFilter가 status/온보딩/role/정지기한을 최신값으로 재확인하는 데 쓴다.
    @Query("SELECT u.status AS status, u.loginId AS loginId, u.emailVerified AS emailVerified, u.role AS role, " +
            "u.suspendedUntil AS suspendedUntil FROM User u WHERE u.id = :userId")
    Optional<UserAuthProjection> findAuthProjectionById(Long userId);

    // Admin 기능
    Page<User> findByNicknameContainingOrEmailContaining(String nickname, String email, Pageable pageable);

    // 룰렛 랜덤 상대 선정용(고정 상대가 없거나 무효화된 경우 매 스핀마다 호출): 본인을 제외한 활성 유저 중 무작위 1명.
    // ROOT는 정지/권한변경 등 다른 관리 기능에서도 보호 대상이라 룰렛 피해자에서도 제외한다.
    // 상대 잔액은 조건에서 제외한다 - RouletteService.spin()이 이미 패자의 실제 잔액을 넘지 않도록
    // 캡을 걸어두므로(actualAmount = min(betAmount, loserBalance)) 잔액 부족은 안전하게 처리되고,
    // 반대로 이 조건이 있으면 신규 계정(잔액 0)이 영원히 매칭·고정 대상이 될 수 없는 부작용만 생긴다.
    // ORDER BY RANDOM()은 대상 row 전체에 난수를 매겨 정렬해야 해서 유저가 많아지면 느려짐 ->
    // COUNT로 대상 수를 센 뒤 그 범위 안에서 OFFSET만 무작위로 골라 정렬 없이 1건만 읽는다.
    @Query(value = """
            SELECT u.id FROM users u
            WHERE u.status = 'ACTIVE' AND u.role <> 'ROOT' AND u.id <> :excludeUserId
              AND u.email_verified = true AND u.roulette_status = true
            OFFSET floor(random() * (
                SELECT COUNT(*) FROM users u2
                WHERE u2.status = 'ACTIVE' AND u2.role <> 'ROOT' AND u2.id <> :excludeUserId
                  AND u2.email_verified = true AND u2.roulette_status = true
            ))::bigint
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findRandomActiveUserIdExcluding(@Param("excludeUserId") Long excludeUserId);

    // 기한이 지난 정지를 자동으로 해제 (경매로 획득한 ADMIN 권한이 부여한 기한부 정지 포함)
    @Modifying
    @Query("update User u set u.status = 'ACTIVE', u.suspendedUntil = null " +
            "where u.status = 'SUSPENDED' and u.suspendedUntil is not null and u.suspendedUntil <= :now")
    int reactivateAllExpiredSuspensions(@Param("now") LocalDateTime now);

    List<User> findAllByRoleIs(UserRole role);
}