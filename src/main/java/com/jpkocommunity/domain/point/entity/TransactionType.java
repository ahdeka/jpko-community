package com.jpkocommunity.domain.point.entity;

public enum TransactionType {
    GRANT, // 시스템 -> 유저 지급
    COLLECT, // 유저 -> 시스템 회수
    ACCOUNT_OPENED,  // 신규 계정 개설 감사 로그 (금액 이동 없음, amount=0)
    ROULETTE // 유저 -> 유저, 일일 포인트 룰렛으로 강탈된 포인트 (amount>=0)
}