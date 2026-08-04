package com.jpkocommunity.domain.shop.scheduler;

import com.jpkocommunity.domain.shop.service.RoleAuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 회차 정산/개설은 5분 주기로 충분하지만, 낙찰로 부여된 ADMIN 권한 회수는
// 지연이 곧 권한 유출 시간이 되므로 1분 주기로 훨씬 촘촘하게 돈다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAuctionScheduler {

    private final RoleAuctionService roleAuctionService;

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void settleExpiredRounds() {
        roleAuctionService.settleExpiredRounds();
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void openNewRounds() {
        roleAuctionService.openRoundsForActiveItems();
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void expireRolePurchases() {
        roleAuctionService.expireRolePurchases();
    }
}
