package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.SlotPlayResponse;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.SlotProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 3릴 슬롯머신(트리플 매치만 당첨). 유저 대 시스템(하우스) 게임 — 룰렛과 달리 상대 유저 매칭이 없다.
@Service
@RequiredArgsConstructor
public class SlotService {

    private final PointService pointService;
    private final UserService userService;
    private final SlotProperties slotProperties;

    @Transactional
    public SlotPlayResponse play(Long userId, long betAmount) {
        if (!slotProperties.isValidBet(betAmount)) {
            throw new CustomException(ErrorCode.INVALID_BET_AMOUNT);
        }

        if (!userService.findById(userId).isEmailVerified()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_SLOT);
        }

        long fee = Math.round(betAmount * slotProperties.feeRate());

        Map<Long, LockedAccount> accounts = pointService.lockAccounts(null, userId);
        LockedAccount system = accounts.get(null);
        LockedAccount user = accounts.get(userId);

        // 수수료+베팅액을 감당할 잔액이 없으면 애초에 시작하지 않는다. 이 검증 덕에 수수료 차감 후
        // 잔액은 항상 최대 손실액(betAmount) 이상이 보장되어 정산 단계에서 별도의 잔액 캡이 필요 없다.
        if (user.point().getBalance() < betAmount + fee) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        if (fee > 0) {
            pointService.move(user, system, fee, TransactionType.SLOT_FEE, "슬롯머신 수수료");
        }

        SlotProperties.RollResult roll = slotProperties.roll();
        long payout = Math.round(betAmount * roll.multiplier());
        long net = payout - betAmount;

        if (net >= 0) {
            pointService.move(system, user, net, TransactionType.SLOT, "슬롯머신 결과");
        } else {
            pointService.move(user, system, -net, TransactionType.SLOT, "슬롯머신 결과");
        }

        return new SlotPlayResponse(betAmount, fee, roll.symbols(), roll.win(),
                roll.multiplier(), payout, net, net > 0, user.point().getBalance());
    }
}
