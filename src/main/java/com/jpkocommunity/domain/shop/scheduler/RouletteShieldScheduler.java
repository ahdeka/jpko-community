package com.jpkocommunity.domain.shop.scheduler;

import com.jpkocommunity.domain.shop.service.RouletteShieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 게이팅 자체는 룰렛 대결/스핀 로직이 User.rouletteStatus를 직접 보고 실시간으로 판단하므로,
// 이 스케줄러는 만료된 방어권을 정리하고 이전 동의 상태로 원복하는 역할이다(ActivityBlockExpiryScheduler와 동일한 위치).
@Component
@RequiredArgsConstructor
public class RouletteShieldScheduler {

    private final RouletteShieldService rouletteShieldService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void expireShields() {
        rouletteShieldService.expireShields();
    }
}
