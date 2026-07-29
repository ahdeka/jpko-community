package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.domain.user.entity.User;
import lombok.Builder;

@Builder
public record TransactionResponse(
        Long transactionId,
        User sender,
        User recipient,
        Long amount
) {
}
