package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.dto.request.TransferRequest;
import com.jpkocommunity.domain.point.dto.response.PointResponse;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.dto.response.TransferResponse;
import com.jpkocommunity.domain.point.entity.GrantType;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.PointRepository;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.TransferProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    private final ApplicationEventPublisher eventPublisher;
    private final TransferProperties transferProperties;

    public PointResponse getPointByUserId(Long userId) {
        return PointResponse.from(getOrCreateAccount(userId));
    }

    public Page<TransactionResponse> getMyTransactions(Long userId, Pageable pageable) {
        User user = userService.findById(userId);
        return transactionRepository.findBySenderOrRecipient(user, pageable)
                .map(TransactionResponse::from);
    }

    public TransferResponse transferPoints(Long senderId, TransferRequest request) {
        // 이메일 인증 받은 유저만 송금 기능 활성화
//        if (!userService.findById(senderId).isEmailVerified() || !userService.findById(request.recipientId()).isEmailVerified()) {
//            throw new CustomException(ErrorCode.USER_ID_NOT_FOUND);
//        }
        try {

            if (request.amount() <= 0) {
                throw new CustomException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
            }

            Map<Long, LockedAccount> accounts = lockAccounts(senderId, request.recipientId(), null);
            LockedAccount sender = accounts.get(senderId);
            LockedAccount recipient = accounts.get(request.recipientId());
            LockedAccount system = accounts.get(null);

            long transferFee = transferProperties.rollAmount();

            TransactionResponse fee;
            if (transferFee >= 0) {
                if (sender.point().getBalance() <= request.amount() + transferFee) {
                    throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
                }
                fee = move(sender, system, transferFee, TransactionType.TRANSFER_FEE, "RANDOM TRANSFER FEE WAS " + transferFee);

            } else {
                if (sender.point().getBalance() - transferFee <= request.amount()) {
                    throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
                }

                fee = move(system, sender, -transferFee, TransactionType.TRANSFER_FEE, "RANDOM TRANSFER FEE WAS " + transferFee);
            }

            TransactionResponse transfer = move(sender, recipient, request.amount(), TransactionType.TRANSFER, null);

            eventPublisher.publishEvent(new NotificationEvent(
                    request.recipientId(), senderId, NotificationType.TRANSFER, null, null, false, request.amount()
            ));

            return TransferResponse.builder()
                    .status(HttpStatus.ACCEPTED)
                    .transfer(transfer)
                    .fee(fee)
                    .build();

        } catch (CustomException e) {
            return TransferResponse.builder()
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .code(e.getErrorCode())
                    .transfer(null)
                    .fee(null)
                    .e(e.getMessage())
                    .build();
        } catch (Exception e) {
            return TransferResponse.builder()
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .e(e.getMessage())
                    .build();
        }
    }

    // 유저 -> 시스템. 상점 등 다른 도메인이 포인트 소비 로직을 재구현하지 않고 이 메서드만 호출하도록 한다.
    public TransactionResponse charge(Long userId, long amount, TransactionType type, String message) {
        Map<Long, LockedAccount> accounts = lockAccounts(userId, null);
        return move(accounts.get(userId), accounts.get(null), amount, type, message);
    }

    // 시스템 -> 유저. 입찰 낙찰 실패/취소 시 예탁금 환불, 차단 관련 환불 등에 사용
    public TransactionResponse refund(Long userId, long amount, TransactionType type, String message) {
        Map<Long, LockedAccount> accounts = lockAccounts(userId, null);
        return move(accounts.get(null), accounts.get(userId), amount, type, message);
    }

    public TransactionResponse grantPointByActivity(Long userId, long amount, String message, Long postId, Long commentId, GrantType grantType) {
        // 포인트 리워드는 이메일 인증을 받은 유저만
        if (!userService.findById(userId).isEmailVerified()) {
            return null;
        }

        Map<Long, LockedAccount> accounts = lockAccounts(null, userId);
        LockedAccount system = accounts.get(null);
        LockedAccount target = accounts.get(userId);

        TransactionResponse res = move(system, target, amount, TransactionType.GRANT, message);

        NotificationType notificationType = switch (grantType) {
            case LIKE -> NotificationType.LIKE_REWARD;
            case POST -> NotificationType.POST_REWARD;
            case COMMENT -> NotificationType.COMMENT_REWARD;
        };

        eventPublisher.publishEvent(new NotificationEvent(
                userId, null, notificationType, postId, commentId, false, amount
        ));

        return res;
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
    // public이어야 한다 - Spring의 기본 트랜잭션 프록시는 public 메서드만 인터셉트하므로,
    // package-private이면 이 @Transactional은 조용히 무시된다(지금은 항상 이미 열린 앰비언트
    // 트랜잭션 안에서만 호출돼 우연히 문제가 없었을 뿐).
    @Transactional
    public Map<Long, LockedAccount> lockAccounts(Long... userIds) {
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
    // public 이유는 lockAccounts() 주석 참고.
    @Transactional
    public TransactionResponse move(LockedAccount from, LockedAccount to, long amount, TransactionType type, String message) {
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
        try {
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
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
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