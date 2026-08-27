package com.jpkocommunity.domain.point.repository;

import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // sender/recipient를 fetch join으로 함께 가져와 목록 조회 N+1 방지
    @Query("""
            select t from Transaction t
            left join fetch t.sender
            left join fetch t.recipient
            where t.sender = :user or t.recipient = :user
            """)
    Page<Transaction> findBySenderOrRecipient(@Param("user") User user, Pageable pageable);

    // 유저가 관여한 거래가 since 이후로 존재하는지 확인
    // -> 출첵 이미 했는지 등에 사용
    @Query("""
            select case when count(t) > 0 then true else false end from Transaction t
            where t.type = :type and t.createdAt >= :since and 
            (t.sender.id = :userId or t.recipient.id = :userId)
            """)
    boolean existsByUserAndTypeSince(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("since") LocalDateTime since
    );

    // since 이후 유저가 활동 보상으로 실제 지급받은 합계
    // 오늘 활동으로 번 포인트 조회 에서 일일상한 계산에 사용
    @Query("""
        select coalesce(sum(t.amount), 0) from Transaction t
        where t.type = com.jpkocommunity.domain.point.entity.TransactionType.GRANT
          and t.recipient.id = :userId
          and t.createdAt >= :since
        """)
    long sumActivityGrantSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
