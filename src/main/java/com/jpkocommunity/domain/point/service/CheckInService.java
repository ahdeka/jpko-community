package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
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

// 로그인(및 회원가입 직후 자동 로그인) 시 하루 1회, 시스템 계정을 상대로 마이너스~플러스 범위에서
// 랜덤하게 포인트를 증감하는 출첵
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final TransactionRepository transactionRepository;
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
            // 락 건 뒤 재확인한 결과 오늘 이미 출첵한 상태였다면(동시 로그인 레이스) empty로 돌아온다.
            // ⚠️ TransactionResponse.amount()는 원장 관례상 항상 양수(방향은 sender/recipient로 구분)라,
            // 그대로 넘기면 마이너스 결과도 "획득"으로 보이는 버그가 생긴다 -> recipient가 나 자신인지로
            // 부호를 다시 살려서 CheckInOutcome에 담는다.
            return transactionService.checkIn(userId, rolledAmount, "일일 출첵")
                    .map(tx -> CheckInOutcome.checkedIn(
                            userId.equals(tx.recipientId()) ? tx.amount() : -tx.amount()))
                    .orElseGet(CheckInOutcome::none);
        } catch (CustomException e) {
            // 유저가 처리 도중 정지되는 등 드문 레이스 -> 스킵 처리
            log.warn("[출첵] 처리 실패, 스킵 처리. userId={}", userId, e);
            return CheckInOutcome.none();
        }
    }

    private boolean alreadyCheckedInToday(Long userId) {
        return transactionRepository.existsByRecipientIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, TransactionType.CHECKIN, kstDateUtils.startOfToday())
                || transactionRepository.existsBySenderIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, TransactionType.CHECKIN, kstDateUtils.startOfToday());
    }
}
