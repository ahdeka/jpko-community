package com.jpkocommunity.domain.auth.repository;

import com.jpkocommunity.domain.auth.entity.VerificationToken;
import com.jpkocommunity.domain.auth.entity.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    // 재요청 (인증 메일 재발송, 재설정 링크 재요청) 시 기존 미사용 토큰의 무효화
    void deleteByUserIdAndType(Long userId, VerificationTokenType type);

    // 만료된 토큰 삭제
    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}