package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.PointResponse;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.PointRepository;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 계정에 비관적 잠금을 건 채로 조회
    // 없으면 생성
    @Transactional
    Point lockAccountForUpdate(Long userId) {
        if (userId == null) {
            return pointRepository.findSystemAccountForUpdate()
                    .orElseThrow(() -> new CustomException(ErrorCode.SYSTEM_ACCOUNT_NOT_FOUND));
        }
        User user = userService.findById(userId);
        return pointRepository.findByUserForUpdate(user)
                .orElseGet(() -> createAccount(user));
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