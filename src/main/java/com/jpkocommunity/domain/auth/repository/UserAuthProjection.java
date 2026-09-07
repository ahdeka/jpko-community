package com.jpkocommunity.domain.auth.repository;

import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;

/**
 * 매 요청마다 JwtFilter가 조회할 최소 필드
 */
public interface UserAuthProjection {

    UserStatus getStatus();

    // role은 JWT 토큰에 로그인 시점 값이 박혀있지만, 그 값을 그대로 쓰면 권한 변경(경매 만료,
    // ROOT의 즉시 박탈 등)이 토큰이 만료될 때까지 반영되지 않는다. status와 마찬가지로
    // 매 요청마다 최신 값을 여기서 다시 확인해 JwtAuthenticationFilter가 그 값을 쓰게 한다.
    UserRole getRole();
}
