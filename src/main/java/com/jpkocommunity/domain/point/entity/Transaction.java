package com.jpkocommunity.domain.point.entity;

import com.jpkocommunity.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 포인트 증감 거래 기록
@Getter
@Entity
@Table(name = "transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null이면 시스템 계정에서 차감됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    // null이면 시스템 계정으로 적립됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Long prevSenderBalance;
    private Long postSenderBalance;
    private Long prevRecipientBalance;
    private Long postRecipientBalance;

    private String message;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Transaction(
            User sender, User recipient, Long amount, TransactionType type,
            Long prevSenderBalance, Long postSenderBalance,
            Long prevRecipientBalance, Long postRecipientBalance,
            String message
    ) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.type = type;
        this.prevSenderBalance = prevSenderBalance;
        this.postSenderBalance = postSenderBalance;
        this.prevRecipientBalance = prevRecipientBalance;
        this.postRecipientBalance = postRecipientBalance;
        this.message = message;
    }
}