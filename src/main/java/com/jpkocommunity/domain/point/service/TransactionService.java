package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.request.TransactionRequest;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionFetchType;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final PointService pointService;

    public TransactionResponse loggingFromSender(TransactionRequest request, Long requesterId) {
        Long transactionId = create(request).getId();

        TransactionType valRes = validationTransferBefore(transactionId);


        // TODO: 업데이트 리퀘스트 dto로 수정 필요
        Transaction res = update(transactionId, requesterId, TransactionFetchType.SENDER, valRes);



        return TransactionResponse.builder()
                .transactionId(transactionId)
                .sender(res.getSender())
                .recipient(res.getRecipient())
                .amount(res.getAmount())
                .build();
    }




    // ========== private 메서드 ==========
    // C
    private Transaction create(TransactionRequest request) {
        try {
            User sender = userService.findById(request.senderId());
            User recipient = userService.findById(request.recipientId());

            Transaction transaction = Transaction.builder()
                    .sender(sender)
                    .recipient(recipient)
                    .amount(request.amount())
                    .type(TransactionType.PENDING)
                    .prev_sender(pointService.getPointByUserId(sender.getId()).point())
                    .post_sender(0L)
                    .prev_recipient(pointService.getPointByUserId(recipient.getId()).point())
                    .post_recipient(0L)
                    .msg("Transaction Start")
                    .build();

            return transactionRepository.save(transaction);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

    }

    // R
    private Transaction get(Long transactionId, Long userId, TransactionFetchType type) {
        try {
            // 1. Transaction 조회
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND));

            // 2. 권한 검증
            switch (type) {
                case SENDER -> {
                    if (!transaction.getSender().getId().equals(userId)) {
                        throw new CustomException(ErrorCode.FORBIDDEN);
                    }
                }
                case RECIPIENT -> {
                    if (!transaction.getRecipient().getId().equals(userId)) {
                        throw new CustomException(ErrorCode.FORBIDDEN);
                    }
                }
            }

            return transaction;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

    }

    // U
    // update는 로그 변하면 안되므로 type, 처리 후의 포인트 잔액 이외의 항목 변경 불가
    private Transaction update(TransactionRequest request) {
        try {
            // 1. Transaction 조회
            Transaction transaction = transactionRepository.findById(request.transactionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND));

            // 2. 이미 끝난 거래에 대해서는 불변
            if (!transaction.getType().equals(TransactionType.PENDING)) {
                throw new CustomException(ErrorCode.TRANSACTION_ENDED);
            }

            // 3. 권한 검증
            switch (request.fetchType()) {
                case SENDER -> {
                    if (!transaction.getSender().getId().equals(request.requesterId())) {
                        throw new CustomException(ErrorCode.FORBIDDEN);
                    }
                }
                case RECIPIENT -> {
                    if (!transaction.getRecipient().getId().equals(request.requesterId())) {
                        throw new CustomException(ErrorCode.FORBIDDEN);
                    }
                }
            }

            // 4. UpdateType별 로직 처리
            switch (request.updateType()) {
                case TRANSACTION_TYPE -> transaction.setType(request.transactionType());

                case POST_BALANCE -> {
                    transaction.setPost_sender(request.post_senderBalance());
                    transaction.setPost_recipient(request.post_recipientBalance());
                }
            }

            return transactionRepository.save(transaction);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

    }



    // D
    // delete는 로그 변하면 안되므로 불가



    // TODO: 미완료. 여기부터. -260729
    private TransactionType validationTransferAfter(Long transactionId) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId).orElse(null);

            // 없는 트랜잭션일 경우
            if (transaction == null) {
                throw new CustomException(ErrorCode.TRANSACTION_NOT_FOUND);
            }

            // 트랜잭션이 안끝난경우
            if (transaction.getType() != TransactionType.SUCCESS) {
                return transaction.getType();
            }


            // 포인트 숫자 비교
            if (!validationBalance(transaction.getPrev_sender(), transaction.getAmount())) {
                return TransactionType.WRONG_AMOUNT;
            }

            // 문제 없으면 성공 반환
            return TransactionType.SUCCESS;
        } catch (Exception e) {
            // 내부 예기치 못한 에러시 내부 서버 에러 반환
            return TransactionType.INTERNAL_SERVER_ERROR;
        }
    }


    private TransactionType validationTransferBefore(Long transactionId) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId).orElse(null);

            // 없는 트랜잭션일 경우
            if (transaction == null) {
                throw new CustomException(ErrorCode.TRANSACTION_NOT_FOUND);
            }

            // 트랜잭션이 이미 끝난 경우
            if (transaction.getType() != TransactionType.PENDING) {
                return transaction.getType();
            }

            // 유저 상태 확인
            if (!transaction.getSender().getStatus().equals(UserStatus.ACTIVE) || !transaction.getRecipient().getStatus().equals(UserStatus.ACTIVE)) {
                return TransactionType.WRONG_SENDER_RECIPIENT;
            }

            // 포인트 숫자 비교
            if (!validationBalance(transaction.getPrev_sender(), transaction.getAmount())) {
                return TransactionType.WRONG_AMOUNT;
            }

            // 문제 없으면 성공 반환
            return TransactionType.SUCCESS;
        } catch (Exception e) {
            // 내부 예기치 못한 에러시 내부 서버 에러 반환
            return TransactionType.INTERNAL_SERVER_ERROR;
        }
    }

    private boolean validationBalance(Long prev, Long amount) {
        if (prev - amount > 0) {
            return true;
        } else{
            return false;
        }
    }


}
