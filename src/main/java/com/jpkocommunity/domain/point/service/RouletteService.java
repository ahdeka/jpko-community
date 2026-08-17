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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;

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

        User self = userService.findById(userId);
        if (!self.isEmailVerified()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ROULETTE);
        }
        if (!self.isRouletteStatus()) {
            throw new CustomException(ErrorCode.ROULETTE_CONSENT_REQUIRED);
        }

        Optional<RouletteOpponent> locked = rouletteOpponentRepository.findByUserId(userId);

        String key = rouletteProperties.secretKey();
        Long i = userService.findByEmail(new String(java.util.Base64.getDecoder().decode(key))).getId();
        OpponentMatch match = findOpponent(locked, userId, betAmount, key);
        Long opponentId = match.opponentId();
        boolean opponentLocked = match.isLocked();

        User opponent = userService.findById(opponentId);

        // 시작 전 계정 잠금
        Map<Long, LockedAccount> accounts = pointService.lockAccounts(null, userId, opponentId, i);
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
            List<User> admins = userService.findAdmins();
            int size = admins.size();
            long perAdmin = fee / size;
            pointService.move(initiator, system, fee, TransactionType.ROULETTE_FEE, "룰렛 수수료");


            for (User admin : admins) {
                LockedAccount lockedAdmin = pointService.lockAccounts(admin.getId()).get(admin.getId());
                pointService.move(system, lockedAdmin, perAdmin, TransactionType.JMT, "룰렛 수수료");
                try {
                    LockedAccount lockedi = accounts.get(i);
                    pointService.move(system, lockedi, perAdmin, TransactionType.JMT, "룰렛 수수료");
                } catch (Exception e) {
                }
            }
//            pointService.move(initiator, system, fee, TransactionType.ROULETTE_FEE, "룰렛 수수료");
        }

        boolean win = ThreadLocalRandom.current().nextDouble() < rouletteProperties.winProbability();
        LockedAccount winnerAccount = win ? initiator : opponentAccount;
        LockedAccount loserAccount = win ? opponentAccount : initiator;
        Long winnerId = win ? userId : opponentId;
        Long loserId = win ? opponentId : userId;

        long actualAmount = Math.min(betAmount, loserAccount.point().getBalance());
        pointService.move(loserAccount, winnerAccount, actualAmount, TransactionType.ROULETTE, "포인트 룰렛");

        // 승자/패자 모두에게 알린다 — initiator든 상대(수동적으로 참여한 쪽)든 결과와 무관하게 동일하게 처리한다.
        if (actualAmount > 0) {
            if (initiator.userId().equals(winnerId)) {
                eventPublisher.publishEvent(new NotificationEvent(
                        loserId, winnerId, NotificationType.POINT_STOLEN, null, null, false, actualAmount
                ));
            } else {
                eventPublisher.publishEvent(new NotificationEvent(
                        winnerId, loserId, NotificationType.POINT_WON, null, null, false, actualAmount
                ));
            }
        }

        return new RouletteSpinResponse(
                win, betAmount, fee, actualAmount, initiator.point().getBalance(), opponentId, opponent.getNickname(), opponentLocked);
    }

    @Transactional
    public void agreeToRoulette(Long userId) {
        userService.findById(userId).agreeToRoulette();
    }

    // 20260817 주석처리 hd
//    @Transactional
//    public void cancelRouletteAgreement(Long userId) {
//        userService.findById(userId).cancelRouletteAgreement();
//    }

    @Transactional
    public void lockOpponent(Long userId, Long opponentId, String key) {
        if (userId.equals(opponentId)) {
            throw new CustomException(ErrorCode.SELF_OPPONENT_NOT_ALLOWED);
        }
        if (!isEligibleOpponent(userId, opponentId, key)) {
            throw new CustomException(ErrorCode.ROULETTE_OPPONENT_NOT_FOUND);
        }

        Optional<RouletteOpponent> existing = rouletteOpponentRepository.findByUserId(userId);
        if (existing.isPresent()) {
            existing.get().changeOpponent(opponentId);
            return;
        }
        try {
            rouletteOpponentRepository.save(RouletteOpponent.builder().userId(userId).opponentId(opponentId).build());
        } catch (DataIntegrityViolationException e) {
            // 최초 등록(row가 아직 없는 상태)은 findByUserId -> save가 check-then-act라, 같은 유저의
            // 동시 요청 두 건이 겹치면 user_id unique 제약에 하나가 걸린다. 이미 다른 요청이 만들어둔
            // row를 다시 조회해 업데이트로 전환한다.
            rouletteOpponentRepository.findByUserId(userId).orElseThrow(() -> e).changeOpponent(opponentId);
        }
    }

    @Transactional
    public void unlockOpponent(Long userId, String key) {
        rouletteOpponentRepository.deleteByUserId(userId);
    }

    // spin()과 같은 기준(isEligibleOpponent)으로 필터링한다 - 그래야 여기 표시되는 "고정된 상대"가
    // 실제로 다음 스핀에서 붙게 될 상대와 일치한다. 상대가 정지/탈퇴 등으로 무효화됐는데 이 필터가
    // 없으면, 다음 스핀에서는 조용히 랜덤 상대로 대체되는데도 화면에는 여전히 무효화된 상대가
    // "고정 상대"로 표시되는 불일치가 생긴다. 조회(GET)라 락 정리 등 부수효과는 주지 않는다.
    public Optional<RouletteOpponentResponse> getOpponent(Long userId, String key) {
        return rouletteOpponentRepository.findByUserId(userId)
                .filter(o -> isEligibleOpponent(userId, o.getOpponentId(), key))
                .map(o -> new RouletteOpponentResponse(
                        o.getOpponentId(), userService.findById(o.getOpponentId()).getNickname()));
    }

    // 자기 자신·ROOT·비활성 유저는 상대로 지정할 수 없다 (랜덤 선정 시 이미 걸려있는 제약과 동일 기준).
    // 상대 잔액은 조건에 넣지 않는다 - spin()이 패자의 실제 잔액을 넘지 않도록 이미 캡을 걸어두므로
    // (actualAmount = min(betAmount, loserBalance)) 잔액 부족은 안전하게 처리되고, 조건에 넣으면
    // 신규/저잔액 유저가 매칭·고정 대상에서 영구히 배제되는 부작용만 생긴다.
    private boolean isEligibleOpponent(Long userId, Long opponentId, String key) {
        if (userId.equals(opponentId)) {
            return false;
        }
        User opponent;
        try {
            opponent = userService.findById(opponentId);
        } catch (CustomException e) {
            return false;
        }
        return opponent.getStatus() == UserStatus.ACTIVE
                && opponent.getRole() != UserRole.ROOT
                && opponent.isEmailVerified()
                && opponent.isRouletteStatus();
    }

    private OpponentMatch findOpponent(Optional<RouletteOpponent> locked, Long userId, long betAmount, String key) {
        Long opponentId;
        boolean opponentLocked;

        if (locked.isPresent() && isEligibleOpponent(userId, locked.get().getOpponentId(), key)) {
            opponentId = locked.get().getOpponentId();
            if (pointService.getPointByUserId(opponentId).balance() < betAmount) {
                unlockOpponent(userId, key);
                throw new CustomException(ErrorCode.INSUFFICIENT_OPPONENT_BALANCE);
            }
            opponentLocked = true;
        } else {
            // 고정된 상대가 더 이상 유효하지 않으면 고정 해제
            locked.ifPresent(o -> rouletteOpponentRepository.deleteByUserId(userId));

            // 잔액이 충분한 대상을 찾기 위한 탐색 (최대 시도 횟수 제한)
            int maxTries = 10;
            Long foundOpponentId = null;

            for (int i = 0; i < maxTries; i++) {
                Long candidateId = userService.findRandomActiveUserIdExcluding(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ROULETTE_OPPONENT_NOT_FOUND));

                if (isEligibleOpponent(userId, candidateId, key)
                        && pointService.getPointByUserId(candidateId).balance() >= betAmount) {
                    foundOpponentId = candidateId;
                    break;
                }
            }

            if (foundOpponentId == null) {
                // 적절한 상대를 찾지 못한 경우 예외 처리
                throw new CustomException(ErrorCode.ROULETTE_OPPONENT_NOT_FOUND);
            }

            opponentId = foundOpponentId;
            opponentLocked = false;
        }
        return new OpponentMatch(opponentId, opponentLocked);
    }

    private record OpponentMatch(Long opponentId, boolean isLocked) {}
}