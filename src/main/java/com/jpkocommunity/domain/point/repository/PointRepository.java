package com.jpkocommunity.domain.point.repository;

import com.jpkocommunity.domain.point.dto.response.RankingRow;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {

    Optional<Point> findByUser(User user);

    Optional<Point> findByUserIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p where p.user = :user")
    Optional<Point> findByUserForUpdate(@Param("user") User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p where p.user is null")
    Optional<Point> findSystemAccountForUpdate();

    // 상위 랭킹
    @Query(value = """
        SELECT u.id AS userId, u.nickname AS nickname, p.balance AS balance
        FROM points p
        JOIN users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE'
        ORDER BY p.balance DESC, u.id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<RankingRow> findTopRanking(@Param("limit") int limit);

    // 내 랭킹 엔트리
    @Query(value = """
        SELECT u.id AS userId, u.nickname AS nickname, p.balance AS balance
        FROM points p
        JOIN users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE' AND u.id = :userId
        """, nativeQuery = true)
    Optional<RankingRow> findMyRankedEntry(@Param("userId") Long userId);

    // 나보다 앞선 사람 수 (rank = 결과 + 1)
    @Query(value = """
        SELECT COUNT(*)
        FROM points p
        JOIN users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE'
          AND (p.balance > :myBalance
               OR (p.balance = :myBalance AND u.id < :myUserId))
        """, nativeQuery = true)
    long countAbove(@Param("myBalance") long myBalance, @Param("myUserId") long myUserId);

    // 전체 랭킹 인원수
    @Query(value = """
        SELECT COUNT(*)
        FROM points p
        JOIN users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE'
        """, nativeQuery = true)
    long countRanked();

    // 나보다 위 k명
    @Query(value = """
        SELECT u.id AS userId, u.nickname AS nickname, p.balance AS balance
        FROM points p
        JOIN users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE'
          AND (p.balance > :myBalance
               OR (p.balance = :myBalance AND u.id < :myUserId))
        ORDER BY p.balance ASC, u.id DESC
        LIMIT :k
        """, nativeQuery = true)
    List<RankingRow> findAbove(@Param("myBalance") long myBalance,
                               @Param("myUserId") long myUserId,
                               @Param("k") int k);

    // 나보다 아래 k명
    @Query(value = """
        SELECT u.id AS userId, u.nickname AS nickname, p.balance AS balance
        FROM points p
        JOIN users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE'
          AND (p.balance < :myBalance
               OR (p.balance = :myBalance AND u.id > :myUserId))
        ORDER BY p.balance DESC, u.id ASC
        LIMIT :k
        """, nativeQuery = true)
    List<RankingRow> findBelow(@Param("myBalance") long myBalance,
                               @Param("myUserId") long myUserId,
                               @Param("k") int k);

}