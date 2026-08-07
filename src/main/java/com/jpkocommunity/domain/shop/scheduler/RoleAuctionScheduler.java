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

    // 회차마다 독립된 트랜잭션(settleRoundSafely)에서 정산한다 - 한 회차의 정산 실패(예: 낙찰
    // 실패자가 정지/탈퇴 상태)가 같은 tick의 다른 회차 정산까지 막지 않도록 하기 위함.
    // catch는 반드시 여기(settleRoundSafely의 트랜잭션 경계 바깥)에서 해야 한다 - 그 메서드 안에서
    // catch하면 이미 rollback-only로 마킹된 트랜잭션이 정상 반환 후 커밋을 시도하다
    // UnexpectedRollbackException을 새로 던져서 여기까지 그대로 전파되어 이 반복문을 끊어버린다.
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void settleExpiredRounds() {
        roleAuctionService.findExpiredRoundIds().forEach(this::settleRoundSafely);
    }

    private void settleRoundSafely(Long roundId) {
        try {
            roleAuctionService.settleRoundSafely(roundId);
        } catch (Exception e) {
            log.error("경매 회차 정산 실패, 다음 tick에 재시도 - round={}", roundId, e);
        }
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void openNewRounds() {
        roleAuctionService.openRoundsForActiveItems();
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void expireRolePurchases() {
        roleAuctionService.expireRolePurchases();
    }
}
