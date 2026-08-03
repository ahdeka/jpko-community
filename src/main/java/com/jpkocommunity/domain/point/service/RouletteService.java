package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.RouletteOpponentResponse;
import com.jpkocommunity.domain.point.dto.response.RouletteSpinResponse;
import com.jpkocommunity.domain.point.entity.RouletteOpponent;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.RouletteOpponentRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.RouletteProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

// 유저가 직접 배팅해 상대와 겨루는 룰렛. 처음엔 랜덤 상대, 이후 원하면 고정/해제할 수 있다.
@Service
@RequiredArgsConstructor
public class RouletteService {

    private final RouletteOpponentRepository rouletteOpponentRepository;
    private final PointService pointService;
    private final UserService userService;
    private final RouletteProperties rouletteProperties;
    private final ApplicationEventPublisher eventPublisher;

    // 로그인 토큰 발급과 얽혀있지 않은 유저 액션이라 REQUIRES_NEW로 쪼갤 필요 없이
    // 수수료 징수 + 대결 정산을 하나의 트랜잭션에서 원자적으로 처리한다.
    @Transactional
    public RouletteSpinResponse spin(Long userId, long betAmount) {
        long fee = Math.round(betAmount * rouletteProperties.feeRate());

        if (!rouletteProperties.isValidBet(betAmount)) {
            throw new CustomException(ErrorCode.INVALID_BET_AMOUNT);
        }

        if (!userService.findById(userId).isEmailVerified()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ROULETTE);
        }

        Optional<RouletteOpponent> locked = rouletteOpponentRepository.findByUserId(userId);
        boolean opponentLocked;
        Long opponentId;
        if (locked.isPresent() && isEligibleOpponent(userId, locked.get().getOpponentId(), betAmount)) {
            opponentId = locked.get().getOpponentId();
            opponentLocked = true;
        } else {
            // 고정된 상대가 더 이상 유효하지 않으면(정지/탈퇴 등) 고정을 풀고 랜덤으로 대체한다.
            locked.ifPresent(o -> rouletteOpponentRepository.deleteByUserId(userId));
            opponentId = userService.findRandomActiveUserIdExcluding(userId, betAmount)
                    .orElseThrow(() -> new CustomException(ErrorCode.ROULETTE_OPPONENT_NOT_FOUND));
            opponentLocked = false;
        }

        User opponent = userService.findById(opponentId);

        // 시작 전 계정 잠금
        Map<Long, LockedAccount> accounts = pointService.lockAccounts(null, userId, opponentId);
        LockedAccount system = accounts.get(null);
        LockedAccount initiator = accounts.get(userId);
        LockedAccount opponentAccount = accounts.get(opponentId);

        // 배팅액+수수료를 감당할 잔액이 없으면 애초에 시작하지 않는다. 이 검증이 없으면
        // 수수료를 먼저 뗀 뒤 패배해서 잔액이 모자랄 때, settleRouletteDuel의 Math.min 캡 때문에
        // 원래 배팅액보다 적게 잃는(사실상 할인되는) 의도치 않은 결과가 생길 수 있다.
        if (initiator.point().getBalance() < betAmount + fee) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        if (fee > 0) {
            pointService.move(initiator, system, fee, TransactionType.ROULETTE_FEE, "룰렛 수수료");
        }

        boolean win = ThreadLocalRandom.current().nextDouble() < rouletteProperties.winProbability();
        LockedAccount winnerAccount = win ? initiator : opponentAccount;
        LockedAccount loserAccount = win ? opponentAccount : initiator;
        Long winnerId = win ? userId : opponentId;
        Long loserId = win ? opponentId : userId;

        long actualAmount = Math.min(betAmount, loserAccount.point().getBalance());
        pointService.move(loserAccount, winnerAccount, actualAmount, TransactionType.ROULETTE, "포인트 룰렛");

        // 승자/패자 모두에게 알린다 — initiator든 상대(수동적으로 참여한 쪽)든 결과와 무관하게 동일하게 처리한다.
        // (initiator는 이 스핀 응답으로 결과를 이미 알지만, 알림 목록/벨에도 동일하게 남도록 별도로 발행한다)
        if (actualAmount > 0) {
            eventPublisher.publishEvent(new NotificationEvent(
                    loserId, winnerId, NotificationType.POINT_STOLEN, null, null, false, actualAmount
            ));
            eventPublisher.publishEvent(new NotificationEvent(
                    winnerId, loserId, NotificationType.POINT_WON, null, null, false, actualAmount
            ));
        }

        return new RouletteSpinResponse(
                win, betAmount, fee, actualAmount, initiator.point().getBalance(), opponentId, opponent.getNickname(), opponentLocked);
    }

    @Transactional
    public void lockOpponent(Long userId, Long opponentId, long betAmount) {
        long fee = Math.round(betAmount * rouletteProperties.feeRate());

        if (userId.equals(opponentId)) {
            throw new CustomException(ErrorCode.SELF_OPPONENT_NOT_ALLOWED);
        }
        if (!isEligibleOpponent(userId, opponentId, betAmount)) {
            throw new CustomException(ErrorCode.ROULETTE_OPPONENT_NOT_FOUND);
        }

        rouletteOpponentRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.changeOpponent(opponentId),
                        () -> rouletteOpponentRepository.save(
                                RouletteOpponent.builder().userId(userId).opponentId(opponentId).build())
                );
    }

    @Transactional
    public void unlockOpponent(Long userId) {
        rouletteOpponentRepository.deleteByUserId(userId);
    }

    public Optional<RouletteOpponentResponse> getOpponent(Long userId) {
        return rouletteOpponentRepository.findByUserId(userId)
                .map(o -> new RouletteOpponentResponse(
                        o.getOpponentId(), userService.findById(o.getOpponentId()).getNickname()));
    }

    // 자기 자신·ROOT·비활성 유저는 상대로 지정할 수 없다 (랜덤 선정 시 이미 걸려있는 제약과 동일 기준)
    private boolean isEligibleOpponent(Long userId, Long opponentId, Long betAmount) {
        if (userId.equals(opponentId)) {
            return false;
        }
        User opponent;
        try {
            opponent = userService.findById(opponentId);
        } catch (CustomException e) {
            return false;
        }
        return opponent.getStatus() == UserStatus.ACTIVE && opponent.getRole() != UserRole.ROOT && opponent.isEmailVerified() && pointService.getPointByUserId(opponentId).balance() >= betAmount;
    }
}