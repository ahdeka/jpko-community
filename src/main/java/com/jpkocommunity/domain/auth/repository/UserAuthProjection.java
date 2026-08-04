package com.jpkocommunity.domain.auth.repository;

import com.jpkocommunity.domain.user.entity.UserStatus;

/**
 * 매 요청마다 JwtFilter가 조회할 최소 필드
 * TODO 마이그레이션이 끝나면 삭제할 파일
 */
public interface UserAuthProjection {

    UserStatus getStatus();

    String getLoginId();

    boolean isEmailVerified();
}
