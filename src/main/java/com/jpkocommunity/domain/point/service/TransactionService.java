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

    // 유저 -> 시스템 (일일 포인트 룰렛 몰수). 피해자 잔액이 부족하면 잔액만큼만(0 포함) 실제로 차감된다.
    // 포인트 이동은 항상 시스템 계정을 거친다는 원장의 불변식을 지키기 위해 유저-유저 직접 이체 대신
    // "피해자->시스템" / "시스템->당첨자" 2단계로 나눈 첫 번째 레그.
    @Transactional
    public TransactionResponse collectForRoulette(Long victimUserId, long rolledAmount, String message) {
        User victim = userService.findById(victimUserId);
        validateActive(victim);

        // 시스템 계정을 항상 먼저 잠금 -> 기존 execute()와 동일한 데드락 방지 패턴
        Point lockedSystem = pointService.lockAccountForUpdate(null);
        Point lockedVictim = pointService.lockAccountForUpdate(victimUserId);

        long actualAmount = Math.min(rolledAmount, lockedVictim.getBalance()); // 락 이후 계산 -> TOCTOU 없음

        long prevVictimBalance = lockedVictim.getBalance();
        long prevSystemBalance = lockedSystem.getBalance();

        lockedVictim.debit(actualAmount);
        lockedSystem.credit(actualAmount);

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .sender(victim)
                .recipient(null)
                .amount(actualAmount)
                .type(TransactionType.ROULETTE)
                .prevSenderBalance(prevVictimBalance)
                .postSenderBalance(lockedVictim.getBalance())
                .prevRecipientBalance(prevSystemBalance)
                .postRecipientBalance(lockedSystem.getBalance())
                .message(message)
                .build());

        return TransactionResponse.from(transaction);
    }

    // 시스템 -> 유저 (일일 포인트 룰렛 지급): collectForRoulette()로 몰수한 금액을 당첨자에게 지급하는 두 번째 레그.
    // 당첨자 계좌에 락을 건 "이후" 오늘 이미 지급받았는지 재확인한다 -> 동시 로그인(같은 유저, 다른 기기)
    // 경쟁으로 이미 지급이 끝났다면 아무 것도 하지 않고 Optional.empty()를 반환한다(호출부에서 피해자에게 환불 처리).
    @Transactional
    public Optional<TransactionResponse> grantForRouletteIfEligible(Long winnerUserId, long amount, String message) {
        User winner = userService.findById(winnerUserId);
        validateActive(winner);

        Point lockedSystem = pointService.lockAccountForUpdate(null);
        Point lockedWinner = pointService.lockAccountForUpdate(winnerUserId);

        if (transactionRepository.existsByRecipientIdAndTypeAndCreatedAtGreaterThanEqual(
                winnerUserId, TransactionType.ROULETTE, startOfTodayKst())) {
            return Optional.empty();
        }

        long prevWinnerBalance = lockedWinner.getBalance();
        long prevSystemBalance = lockedSystem.getBalance();

        lockedSystem.debit(amount);
        lockedWinner.credit(amount);

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .sender(null)
                .recipient(winner)
                .amount(amount)
                .type(TransactionType.ROULETTE)
                .prevSenderBalance(prevSystemBalance)
                .postSenderBalance(lockedSystem.getBalance())
                .prevRecipientBalance(prevWinnerBalance)
                .postRecipientBalance(lockedWinner.getBalance())
                .message(message)
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