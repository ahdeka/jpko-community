package com.jpkocommunity.global.exception;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class CustomException extends RuntimeException implements Supplier<X> {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}