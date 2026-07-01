package com.jpkocommunity.domain.auth.scheduler;

import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul") // 매일 새벽 4시에 실행
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now());
        log.info("[RefreshToken 정리] 만료 토큰 {}개 삭제 완료", deleted);
    }
}
