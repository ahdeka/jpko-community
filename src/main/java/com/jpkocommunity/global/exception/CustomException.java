package com.jpkocommunity.global.exception;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object data;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    // 에러 응답에 부가 데이터가 필요할 때(예: 정지 계정 안내에 정지 해제 시각을 함께 내려줄 때).
    public CustomException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
}