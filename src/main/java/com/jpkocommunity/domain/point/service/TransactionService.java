package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

// 포인트 원장. 모든 포인트 증감은 이 서비스를 통해서만 이뤄지며,
// 거래 1건마다 시스템 계정과 유저 계정의 잔액 변경 + Transaction 로그 기록이 하나의
// DB 트랜잭션으로 원자적으로 처리된다.
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PointService pointService;
    private final UserService userService;

    // 시스템 계정 -> 유저 (출석 보상, 활동 보상 등)
    @Transactional
    public TransactionResponse grant(Long recipientUserId, Long amount, String message) {
        return execute(null, recipientUserId, amount, TransactionType.GRANT, message);
    }

    // 유저 -> 시스템 (관리자 수동 차감 등)
    @Transactional
    public TransactionResponse collect(Long senderUserId, Long amount, String message) {
        return execute(senderUserId, null, amount, TransactionType.COLLECT, message);
    }

    // 유저 -> 유저 (일일 포인트 룰렛): 피해자(victim) 계좌에서 뺏어 당첨자(winner)에게 지급.
    // rolledAmount는 "뽑힌 금액"이며, 피해자 잔액이 부족하면 잔액만큼만(0 포함) 실제로 차감된다.
    // 오늘 이미 룰렛을 돌린 기록이 있으면(동시 로그인 경쟁) 아무 것도 하지 않고 Optional.empty()를 반환한다.
    @Transactional
    public Optional<TransactionResponse> spinRoulette(Long winnerUserId, Long victimUserId, long rolledAmount) {
        User winner = userService.findById(winnerUserId);
        User victim = userService.findById(victimUserId);
        validateActive(winner);
        validateActive(victim);

        // 락 순서를 user id 오름차순으로 고정 -> 서로를 동시에 피해자로 뽑아도 데드락 없음
        Long firstId = Math.min(winnerUserId, victimUserId);
        Long secondId = Math.max(winnerUserId, victimUserId);
        Point lockedFirst = pointService.lockAccountForUpdate(firstId);
        Point lockedSecond = pointService.lockAccountForUpdate(secondId);
        Point lockedWinner = winnerUserId.equals(firstId) ? lockedFirst : lockedSecond;
        Point lockedVictim = victimUserId.equals(firstId) ? lockedFirst : lockedSecond;

        // 락 획득 이후 재확인 -> 동시 로그인(같은 유저, 다른 기기)으로 이미 오늘 스핀이 기록됐다면 조용히 종료
        if (transactionRepository.existsByRecipientIdAndTypeAndCreatedAtGreaterThanEqual(
                winnerUserId, TransactionType.ROULETTE, startOfTodayKst())) {
            return Optional.empty();
        }

        long actualAmount = Math.min(rolledAmount, lockedVictim.getBalance()); // 락 이후 계산 -> TOCTOU 없음

        long prevVictimBalance = lockedVictim.getBalance();
        long prevWinnerBalance = lockedWinner.getBalance();

        lockedVictim.debit(actualAmount);
        lockedWinner.credit(actualAmount);

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .sender(victim)
                .recipient(winner)
                .amount(actualAmount)
                .type(TransactionType.ROULETTE)
                .prevSenderBalance(prevVictimBalance)
                .postSenderBalance(lockedVictim.getBalance())
                .prevRecipientBalance(prevWinnerBalance)
                .postRecipientBalance(lockedWinner.getBalance())
                .message("일일 포인트 룰렛")
                .build());

        return Optional.of(TransactionResponse.from(transaction));
    }

    public Page<TransactionResponse> getMyTransactions(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return transactionRepository.findBySenderOrRecipient(user, pageable)
                .map(TransactionResponse::from);
    }

    // ========== private 메서드 ==========

    private TransactionResponse execute(
            Long senderUserId, Long recipientUserId, Long amount, TransactionType type, String message
    ) {
        validateAmount(amount);

        // senderUserId/recipientUserId 중 하나는 null
        Long counterpartUserId = senderUserId != null ? senderUserId : recipientUserId;
        User counterpart = userService.findById(counterpartUserId);
        validateActive(counterpart);

        // 시스템 계정을 항상 먼저 잠금 -> 데드락 방지
        Point lockedSystemAccount = pointService.lockAccountForUpdate(null);
        Point lockedUserAccount = pointService.lockAccountForUpdate(counterpartUserId);

        Point lockedSender = senderUserId != null ? lockedUserAccount : lockedSystemAccount;
        Point lockedRecipient = recipientUserId != null ? lockedUserAccount : lockedSystemAccount;

        long prevSenderBalance = lockedSender.getBalance();
        long prevRecipientBalance = lockedRecipient.getBalance();

        lockedSender.debit(amount);
        lockedRecipient.credit(amount);

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .sender(senderUserId != null ? counterpart : null)
                .recipient(recipientUserId != null ? counterpart : null)
                .amount(amount)
                .type(type)
                .prevSenderBalance(prevSenderBalance)
                .postSenderBalance(lockedSender.getBalance())
                .prevRecipientBalance(prevRecipientBalance)
                .postRecipientBalance(lockedRecipient.getBalance())
                .message(message)
                .build());

        return TransactionResponse.from(transaction);
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
        }
    }

    private void validateActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.SUSPENDED_ACCOUNT);
        }
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private LocalDateTime startOfTodayKst() {
        return LocalDate.now(KST).atStartOfDay();
    }
}