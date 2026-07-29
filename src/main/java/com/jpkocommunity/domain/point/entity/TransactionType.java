package com.jpkocommunity.domain.point.entity;

public enum TransactionType {
    SUCCESS,        // 거래 성공
    INTERNAL_SERVER_ERROR,          // 내부 서버 에러로 인한 실패
    WRONG_AMOUNT,           // 거래액 오류(잔고 부족 등)으로 인한 실패
    WRONG_SENDER_RECIPIENT, // 수취인 없음 등으로 인한 실패
    PENDING // 거래 대기
}
