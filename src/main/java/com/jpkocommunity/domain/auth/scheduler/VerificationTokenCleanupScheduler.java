package com.jpkocommunity.domain.auth.scheduler;

import com.jpkocommunity.domain.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationTokenCleanupScheduler {

    private final VerificationTokenRepository verificationTokenRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = verificationTokenRepository.deleteAllExpired(LocalDateTime.now());
        log.info("만료된 verification_tokens 정리 완료 - {}건", deleted);
    }
}