package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.response.PointResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final PointRepository pointRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;

    public PointResponse getPointByUserId(Long userId) {
        return PointResponse.from(getOrCreateAccount(userId));
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


//    // 계정에 비관적 잠금을 건 채로 조회
//    // 없으면 생성
//    @Transactional
//    Point lockAccountForUpdate(Long userId) {
//        if (userId == null) {
//            return pointRepository.findSystemAccountForUpdate()
//                    .orElseThrow(() -> new CustomException(ErrorCode.SYSTEM_ACCOUNT_NOT_FOUND));
//        }
//        User user = userService.findById(userId);
//        return pointRepository.findByUserForUpdate(user)
//                .orElseGet(() -> createAccount(user));
//    }

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

        transactionRepository.save(Transaction.builder()
                .sender(null)
                .recipient(user)
                .amount(0L)
                .type(TransactionType.ACCOUNT_OPENED)
                .prevRecipientBalance(0L)
                .postRecipientBalance(0L)
                .message("Account created")
                .build());

        return account;
    }
}