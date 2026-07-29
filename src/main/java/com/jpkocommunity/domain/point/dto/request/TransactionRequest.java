package com.jpkocommunity.domain.point.dto.request;

import com.jpkocommunity.domain.point.entity.TransactionFetchType;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.entity.TransactionUpdateType;
import lombok.Builder;

@Builder
public record TransactionRequest(
        Long transactionId,
        Long senderId,
        Long recipientId,
        Long amount,
        TransactionType transactionType,
        Long post_senderBalance,
        Long post_recipientBalance,
        Long requesterId,
        TransactionFetchType fetchType,
        TransactionUpdateType updateType
) {
}