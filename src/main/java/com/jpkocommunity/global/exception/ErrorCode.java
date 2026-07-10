package com.jpkocommunity.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    SAME_AS_CURRENT_NICKNAME(HttpStatus.BAD_REQUEST, "현재 닉네임과 동일합니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호와 동일합니다."),
    ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "이미 탈퇴한 계정입니다."),
    SUSPENDED_ACCOUNT(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다."),
    CANNOT_SUSPEND_ADMIN(HttpStatus.BAD_REQUEST, "관리자 계정은 정지할 수 없습니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    POST_EDIT_EXPIRED(HttpStatus.FORBIDDEN, "작성 후 30분이 지난 게시글은 수정할 수 없습니다."),
    TEMP_IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "첨부한 이미지를 찾을 수 없습니다. 이미지를 다시 첨부해주세요."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),

    // Like
    DUPLICATE_LIKE(HttpStatus.CONFLICT, "이미 좋아요/싫어요를 누른 게시글입니다."),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공지사항입니다."),

    // Image
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 이미지입니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "게시글당 이미지는 최대 5장까지 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기는 5MB 이하여야 합니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    // Email Verification
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    EXPIRED_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "만료된 토큰입니다. 다시 요청해주세요."),
    ALREADY_VERIFIED_EMAIL(HttpStatus.CONFLICT, "이미 인증된 이메일입니다."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "존재하지 않는 이메일 도메인입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),

    // Common
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    SEARCH_KEYWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "검색어는 2자 이상 입력해주세요."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
