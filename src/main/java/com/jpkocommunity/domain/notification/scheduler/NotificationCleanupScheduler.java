package com.jpkocommunity.domain.notification.scheduler;

import com.jpkocommunity.domain.notification.repository.NotificationRepository;
import com.jpkocommunity.global.config.properties.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 보관기간 지난 알림 정리 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationProperties notificationProperties;

    @Scheduled(cron = "0 15 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(notificationProperties.retentionDays());
        int deleted = notificationRepository.deleteAllOlderThan(cutoff);
        log.info("[알림 정리] {}일 경과 알림 {}건 삭제 완료", notificationProperties.retentionDays(), deleted);
    }

}
