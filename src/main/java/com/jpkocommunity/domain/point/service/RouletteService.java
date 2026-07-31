package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.point.dto.response.RouletteOpponentResponse;
import com.jpkocommunity.domain.point.dto.response.RouletteSpinResponse;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.RouletteOpponent;
import com.jpkocommunity.domain.point.repository.RouletteOpponentRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.RouletteProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

// 유저가 직접 배팅해 상대와 겨루는 룰렛. 처음엔 랜덤 상대, 이후 원하면 고정/해제할 수 있다.
@Service
@RequiredArgsConstructor
public class RouletteService {

    private final RouletteOpponentRepository rouletteOpponentRepository;
    private final TransactionService transactionService;
    private final PointService pointService;
    private final UserService userService;
    private final RouletteProperties rouletteProperties;
    private final ApplicationEventPublisher eventPublisher;

    // 로그인 토큰 발급과 얽혀있지 않은 유저 액션이라 REQUIRES_NEW로 쪼갤 필요 없이
    // 수수료 징수 + 대결 정산을 하나의 트랜잭션에서 원자적으로 처리한다.
    @Transactional
    public RouletteSpinResponse spin(Long userId, long betAmount) {
        if (!rouletteProperties.isValidBet(betAmount)) {
            throw new CustomException(ErrorCode.INVALID_BET_AMOUNT);
        }

        Optional<RouletteOpponent> locked = rouletteOpponentRepository.findByUserId(userId);
        boolean opponentLocked;
        Long opponentId;
        if (locked.isPresent() && isEligibleOpponent(userId, locked.get().getOpponentId())) {
            opponentId = locked.get().getOpponentId();
            opponentLocked = true;
        } else {
            // 고정된 상대가 더 이상 유효하지 않으면(정지/탈퇴 등) 고정을 풀고 랜덤으로 대체한다.
            locked.ifPresent(o -> rouletteOpponentRepository.deleteByUserId(userId));
            opponentId = userService.findRandomActiveUserIdExcluding(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ROULETTE_OPPONENT_NOT_FOUND));
            opponentLocked = false;
        }

        User opponent = userService.findById(opponentId);

        long fee = Math.round(betAmount * rouletteProperties.feeRate());
        if (fee > 0) {
            transactionService.collectRouletteFee(userId, fee, "룰렛 수수료");
        }

        boolean win = ThreadLocalRandom.current().nextDouble() < rouletteProperties.winProbability();
        Long winnerId = win ? userId : opponentId;
        Long loserId = win ? opponentId : userId;

        TransactionResponse settled = transactionService.settleRouletteDuel(loserId, winnerId, betAmount, "포인트 룰렛");
        long resultAmount = settled.amount();

        // initiator가 이겨서 상대(수동적으로 참여한 쪽)가 뺏긴 경우에만 알린다.
        // initiator가 진 경우는 이 스핀 응답 자체로 이미 결과를 알고 있으므로 별도 알림이 필요 없다.
        if (win && resultAmount > 0) {
            eventPublisher.publishEvent(new NotificationEvent(
                    opponentId, userId, NotificationType.POINT_STOLEN, null, null, false, resultAmount
            ));
        }

        long balanceAfter = pointService.getPointByUserId(userId).balance();

        return new RouletteSpinResponse(
                win, betAmount, fee, resultAmount, balanceAfter, opponentId, opponent.getNickname(), opponentLocked);
    }

    @Transactional
    public void lockOpponent(Long userId, Long opponentId) {
        if (userId.equals(opponentId)) {
            throw new CustomException(ErrorCode.SELF_OPPONENT_NOT_ALLOWED);
        }
        if (!isEligibleOpponent(userId, opponentId)) {
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
    private boolean isEligibleOpponent(Long userId, Long opponentId) {
        if (userId.equals(opponentId)) {
            return false;
        }
        User opponent;
        try {
            opponent = userService.findById(opponentId);
        } catch (CustomException e) {
            return false;
        }
        return opponent.getStatus() == UserStatus.ACTIVE && opponent.getRole() != UserRole.ROOT;
    }
}
