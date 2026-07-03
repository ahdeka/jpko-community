package com.jpkocommunity.domain.auth.repository;

import com.jpkocommunity.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // 로그인 시 "같은 기기"의 토큰만 삭제
    void deleteByUserIdAndDeviceInfo(Long userId, String deviceInfo);

    // 전체 기기 로그아웃
    void deleteByUserId(Long userId);

    /**
     * 만료된 RefreshToken 일괄 삭제
     * @param now 기준 시각 (이보다 이전에 만료된 토큰 삭제)
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteAllExpired(@Param("now")LocalDateTime now);
}