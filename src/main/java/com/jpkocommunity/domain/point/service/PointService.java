package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.PointResponse;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.PointRepository;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 포인트 계좌(Point)와 원장(Transaction)을 함께 관리하는 단일 서비스.
// move(): 포인트 이동(잠금 -> 잔액 변경 -> 원장 기록을 원자적으로 수행)
@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final PointRepository pointRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public PointResponse getPointByUserId(Long userId) {
        return PointResponse.from(getOrCreateAccount(userId));
    }

    public Page<TransactionResponse> getMyTransactions(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return transactionRepository.findBySenderOrRecipient(user, pageable)
                .map(TransactionResponse::from);
    }

    // userId가 null이면 시스템 계정을 반환
    // 잠금 없이 조회만 할 때 사용
    public Point getOrCreateAccount(Long userId) {
        if (userId == null) {
            return getSystemAccount();
        }
        User user = userService.findById(userId);
        return pointRepository.findByUser(user)
                .orElseGet(() -> createAccount(user));
    }

    // 여러 계좌 동시에 락
    // 시스템 계정 -> 유저 id 오름차순으로 잠금
    // 잠그기 전 활성 상태 검증 -> 정지 유저 차단
    @Transactional
    Map<Long, LockedAccount> lockAccounts(Long... userIds) {
        List<Long> lockOrder = Arrays.stream(userIds).distinct()
                .sorted(Comparator.nullsFirst(Comparator.naturalOrder()))
                .toList();

        Map<Long, LockedAccount> locked = new HashMap<>();
        for (Long userId : lockOrder) {
            locked.put(userId, lockAccount(userId));
        }
        return locked;
    }

    // 잠금이 끝난 두 계좌 사이에 잔액 이동 + 원장 기록을 원자적으로 수행하는 포인트 이동
    @Transactional
    TransactionResponse move(LockedAccount from, LockedAccount to, long amount, TransactionType type, String message) {
        if (amount < 0) {
            throw new CustomException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
        }

        long prevFromBalance = from.point().getBalance();
        long prevToBalance = to.point().getBalance();

        from.point().debit(amount);
        to.point().credit(amount);

        Transaction transaction = saveTransaction(
                from.user(), to.user(), amount, type, message,
                prevFromBalance, from.point().getBalance(),
                prevToBalance, to.point().getBalance());
        return TransactionResponse.from(transaction);
    }

    private LockedAccount lockAccount(Long userId) {
        if (userId == null) {
            Point systemPoint = pointRepository.findSystemAccountForUpdate()
                    .orElseThrow(() -> new CustomException(ErrorCode.SYSTEM_ACCOUNT_NOT_FOUND));
            return new LockedAccount(null, systemPoint, null);
        }
        User user = userService.findById(userId);
        validateActive(user);
        Point point = pointRepository.findByUserForUpdate(user)
                .orElseGet(() -> createAccount(user));
        return new LockedAccount(userId, point, user);
    }

    private void validateActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.SUSPENDED_ACCOUNT);
        }
    }

    private Point getSystemAccount() {
        return pointRepository.findByUserIsNull()
                .orElseThrow(() -> new CustomException(ErrorCode.SYSTEM_ACCOUNT_NOT_FOUND));
    }

    @Transactional
    Point createAccount(User user) {
        Point account;
        try {
            account = pointRepository.save(Point.builder().user(user).balance(0L).build());

        } catch (DataIntegrityViolationException e) {
            // 경쟁 상황 -> 이미 다른 트랜잭션이 만든 계정을 그대로 사용
            return pointRepository.findByUser(user)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));
        }

        saveTransaction(null, user, 0L, TransactionType.ACCOUNT_OPENED, "Account created",
                null, null, 0L, 0L);

        return account;
    }

    // 잔액 계산 없이 "이런 내용의 Transaction 한 줄"을 저장하는 것만 담당하는 헬퍼.
    private Transaction saveTransaction(
            User sender, User recipient, long amount, TransactionType type, String message,
            Long prevSenderBalance, Long postSenderBalance,
            Long prevRecipientBalance, Long postRecipientBalance
    ) {
        return transactionRepository.save(Transaction.builder()
                .sender(sender)
                .recipient(recipient)
                .amount(amount)
                .type(type)
                .prevSenderBalance(prevSenderBalance)
                .postSenderBalance(postSenderBalance)
                .prevRecipientBalance(prevRecipientBalance)
                .postRecipientBalance(postRecipientBalance)
                .message(message)
                .build());
    }
}