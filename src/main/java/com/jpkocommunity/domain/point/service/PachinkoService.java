package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.PachinkoBallResult;
import com.jpkocommunity.domain.point.dto.response.PachinkoPlayResponse;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.PachinkoProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 파칭코(플린코형 구슬 낙하). 유저 대 시스템(하우스) 게임 — 룰렛과 달리 상대 유저 매칭이 없다.
// 구슬 1개당 가격(ballPrice)을 정해두고, 유저가 고른 개수(ballCount)만큼 구슬을 각각 독립적으로
// 쏴서 그 결과를 합산해 한 번에 정산한다.
@Service
@RequiredArgsConstructor
public class PachinkoService {

    private final PointService pointService;
    private final UserService userService;
    private final PachinkoProperties pachinkoProperties;

    @Transactional
    public PachinkoPlayResponse play(Long userId, int ballCount) {
        if (!pachinkoProperties.isValidBallCount(ballCount)) {
            throw new CustomException(ErrorCode.INVALID_BALL_COUNT);
        }

        if (!userService.findById(userId).isEmailVerified()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PACHINKO);
        }

        long ballPrice = pachinkoProperties.ballPrice();
        long totalCost = (long) ballCount * ballPrice;
        long fee = Math.round(totalCost * pachinkoProperties.feeRate());

        Map<Long, LockedAccount> accounts = pointService.lockAccounts(null, userId);
        LockedAccount system = accounts.get(null);
        LockedAccount user = accounts.get(userId);

        // 총 구슬 비용+수수료를 감당할 잔액이 없으면 애초에 시작하지 않는다. 이 검증 덕에 수수료 차감 후
        // 잔액은 항상 최대 손실액(totalCost) 이상이 보장되어 정산 단계에서 별도의 잔액 캡이 필요 없다.
        if (user.point().getBalance() < totalCost + fee) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        pointService.move(user, system, betAmount, TransactionType.PACHINKO_FEE, "파칭코 베팅액");

        if (fee > 0) {
            pointService.move(user, system, fee, TransactionType.PACHINKO_FEE, "파칭코 수수료");
        }

        List<PachinkoBallResult> balls = new ArrayList<>(ballCount);
        long totalPayout = 0;
        for (int i = 0; i < ballCount; i++) {
            PachinkoProperties.RollResult roll = pachinkoProperties.roll();
            long payout = Math.round(ballPrice * roll.multiplier());
            totalPayout += payout;
            balls.add(new PachinkoBallResult(roll.bounces(), roll.slotIndex(), roll.multiplier(), payout));
        }

        long net = totalPayout - totalCost;
        if (net >= 0) {
            pointService.move(system, user, net, TransactionType.PACHINKO, "파칭코 결과");
        }

        return new PachinkoPlayResponse(ballCount, ballPrice, totalCost, fee, balls,
                totalPayout, net, net > 0, user.point().getBalance());
    }
}
