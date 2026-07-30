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
}