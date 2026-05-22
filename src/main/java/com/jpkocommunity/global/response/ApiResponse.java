// 파일 경로: src/main/java/com/jpkocommunity/global/response/ApiResponse.java

package com.jpkocommunity.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final String code;
    private final String message;
    private final T data;

    // 200 - 데이터 있음
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .code("SUCCESS")
                .message("요청이 성공했습니다.")
                .data(data)
                .build();
    }

    // 200 - 메시지 커스텀
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    // 200 - 데이터 없음 (삭제 등)
    public static ApiResponse<Void> ok(String message) {
        return ApiResponse.<Void>builder()
                .status(200)
                .code("SUCCESS")
                .message(message)
                .build();
    }

    // 201 - 리소스 생성
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .code("CREATED")
                .message("리소스가 생성되었습니다.")
                .data(data)
                .build();
    }

    // 201 - 메시지 커스텀
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .code("CREATED")
                .message(message)
                .data(data)
                .build();
    }

    // 에러
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.name())
                .message(errorCode.getMessage())
                .build();
    }
}