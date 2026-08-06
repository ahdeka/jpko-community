package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.PachinkoPlayResponse;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.PachinkoProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 파칭코(플린코형 구슬 낙하). 유저 대 시스템(하우스) 게임 — 룰렛과 달리 상대 유저 매칭이 없다.
@Service
@RequiredArgsConstructor
public class PachinkoService {

    private final PointService pointService;
    private final UserService userService;
    private final PachinkoProperties pachinkoProperties;

    @Transactional
    public PachinkoPlayResponse play(Long userId, long betAmount) {
        if (!pachinkoProperties.isValidBet(betAmount)) {
            throw new CustomException(ErrorCode.INVALID_BET_AMOUNT);
        }

        if (!userService.findById(userId).isEmailVerified()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PACHINKO);
        }

        long fee = Math.round(betAmount * pachinkoProperties.feeRate());

        Map<Long, LockedAccount> accounts = pointService.lockAccounts(null, userId);
        LockedAccount system = accounts.get(null);
        LockedAccount user = accounts.get(userId);

        // 수수료+베팅액을 감당할 잔액이 없으면 애초에 시작하지 않는다. 이 검증 덕에 수수료 차감 후
        // 잔액은 항상 최대 손실액(betAmount) 이상이 보장되어 정산 단계에서 별도의 잔액 캡이 필요 없다.
        if (user.point().getBalance() < betAmount + fee) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        if (fee > 0) {
            pointService.move(user, system, fee, TransactionType.PACHINKO_FEE, "파칭코 수수료");
        }

        PachinkoProperties.RollResult roll = pachinkoProperties.roll();
        long payout = Math.round(betAmount * roll.multiplier());
        long net = payout - betAmount;

        if (net >= 0) {
            pointService.move(system, user, net, TransactionType.PACHINKO, "파칭코 결과");
        } else {
            pointService.move(user, system, -net, TransactionType.PACHINKO, "파칭코 결과");
        }

        return new PachinkoPlayResponse(betAmount, fee, roll.bounces(), roll.slotIndex(),
                roll.multiplier(), payout, net, net > 0, user.point().getBalance());
    }
}
