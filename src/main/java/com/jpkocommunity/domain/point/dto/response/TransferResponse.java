package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record TransferResponse(
        HttpStatus status,
        ErrorCode code,
        TransactionResponse transfer,
        TransactionResponse fee,
        String e
) {
}
