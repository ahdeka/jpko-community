package com.jpkocommunity.domain.point.entity;

public enum TransactionType {
    GRANT, // 시스템 -> 유저 지급
    COLLECT, // 유저 -> 시스템 회수
    ACCOUNT_OPENED   // 신규 계정 개설 감사 로그 (금액 이동 없음, amount=0)
}