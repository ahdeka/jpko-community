package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.global.config.CheckInProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.util.KstDateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 로그인(및 회원가입 직후 자동 로그인) 시 하루 1회, 시스템 계정을 상대로 마이너스~플러스 범위에서
// 랜덤하게 포인트를 증감하는 출첵
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final TransactionRepository transactionRepository;
    private final PointService pointService;
    private final TransactionService transactionService;
    private final CheckInProperties checkInProperties;
    private final KstDateUtils kstDateUtils;

    // REQUIRES_NEW -> 호출부인 AuthService.login()의 트랜잭션과 분리해서 독립적으로 커밋한다.
    // 시스템 계정(단일 row)에 락을 거는데, login()의 트랜잭션에 그대로 합류하면
    // 토큰 발급/저장까지 끝날 때까지 그 락을 붙들고 있게 되어 동시 로그인 시 불필요하게 직렬화된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CheckInOutcome checkInIfEligible(Long userId) {
        // 락 없는 사전 체크 -> 하루 중 두 번째 이후 로그인은 대부분 여기서 끝남
        if (alreadyCheckedInToday(userId)) {
            return CheckInOutcome.none();
        }

        long rolledAmount = checkInProperties.rollAmount();

        try {
            Map<Long, LockedAccount> locked = pointService.lockAccounts(null, userId);

            // 락 건 뒤 재확인 -> 동시 로그인(같은 유저, 다른 기기) 경쟁으로 두 스레드 모두 락 없는
            // 사전 체크를 통과했더라도 여기서 걸러져 이중 지급되지 않는다.
            if (alreadyCheckedInToday(userId)) {
                return CheckInOutcome.none();
            }

            LockedAccount system = locked.get(null);
            LockedAccount user = locked.get(userId);

            if (rolledAmount >= 0) {
                TransactionResponse tx = transactionService.record(
                        system, user, rolledAmount, TransactionType.CHECKIN, "일일 출첵");
                return CheckInOutcome.checkedIn(tx.amount());
            }

            long actualAmount = Math.min(-rolledAmount, user.point().getBalance()); // 락 이후 계산 -> TOCTOU 없음
            TransactionResponse tx = transactionService.record(
                    user, system, actualAmount, TransactionType.CHECKIN, "일일 출첵");
            return CheckInOutcome.checkedIn(-tx.amount());
        } catch (CustomException e) {
            // 유저가 처리 도중 정지되는 등 드문 레이스 -> 스킵 처리
            log.warn("[출첵] 처리 실패, 스킵 처리. userId={}", userId, e);
            return CheckInOutcome.none();
        }
    }

    private boolean alreadyCheckedInToday(Long userId) {
        return transactionRepository.existsByUserAndTypeSince(userId, TransactionType.CHECKIN, kstDateUtils.startOfToday());
    }
}