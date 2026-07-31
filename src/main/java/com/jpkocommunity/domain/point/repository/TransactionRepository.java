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

    // "오늘 이미 룰렛을 돌렸는지" 판정용
    boolean existsByRecipientIdAndTypeAndCreatedAtGreaterThanEqual(
            Long recipientId, TransactionType type, LocalDateTime createdAtFrom);
}