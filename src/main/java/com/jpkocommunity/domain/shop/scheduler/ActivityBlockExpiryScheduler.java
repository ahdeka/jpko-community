package com.jpkocommunity.domain.shop.scheduler;

import com.jpkocommunity.domain.shop.repository.ActivityBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 게이팅 자체는 JwtAuthenticationFilter가 만료 시각을 실시간 비교해서 수행하므로
// 이 스케줄러는 데이터 정리/관리자 조회용이며 기능 동작에 필수는 아니다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityBlockExpiryScheduler {

    private final ActivityBlockRepository activityBlockRepository;

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void expireBlocks() {
        int expired = activityBlockRepository.expireAllPast(LocalDateTime.now());
        if (expired > 0) {
            log.info("만료된 activity_blocks 정리 완료 - {}건", expired);
        }
    }
}
