package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;

import java.time.LocalDateTime;

public record TransactionResponse(
        Long transactionId,
        Long senderId,
        String senderNickname,
        Long recipientId,
        String recipientNickname,
        Long amount,
        TransactionType type,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSender() != null ? transaction.getSender().getId() : null,
                transaction.getSender() != null ? transaction.getSender().getNickname() : null,
                transaction.getRecipient() != null ? transaction.getRecipient().getId() : null,
                transaction.getRecipient() != null ? transaction.getRecipient().getNickname() : null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCreatedAt()
        );
    }
}