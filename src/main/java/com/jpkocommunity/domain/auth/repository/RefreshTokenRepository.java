package com.jpkocommunity.domain.auth.repository;

import com.jpkocommunity.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // 로그아웃 / 재로그인 시 기존 토큰 삭제
    void deleteByUserId(Long userId);
}