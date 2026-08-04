package com.jpkocommunity.domain.user.scheduler;

import com.jpkocommunity.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 기한부 정지(주로 경매로 획득한 ADMIN 권한이 부여) 자동 해제.
// 지연이 곧 부당 정지 지속 시간이 되므로 짧은 주기로 돈다.
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSuspensionExpiryScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void reactivateExpiredSuspensions() {
        int reactivated = userRepository.reactivateAllExpiredSuspensions(LocalDateTime.now());
        if (reactivated > 0) {
            log.info("기한부 정지 자동 해제 완료 - {}건", reactivated);
        }
    }
}
