package com.jpkocommunity.domain.point.entity;

public enum TransactionType {
    GRANT, // 시스템 -> 유저 지급
    COLLECT, // 유저 -> 시스템 회수
    ACCOUNT_OPENED,  // 신규 계정 개설 감사 로그 (금액 이동 없음, amount=0)
    CHECKIN, // 시스템 <-> 유저, 매일 로그인 시 자동 출첵. amount는 시스템->유저(GRANT 모양) 또는 유저->시스템(COLLECT 모양) 둘 다 가능
    ROULETTE, // 유저 -> 유저(시스템 경유), 유저가 직접 베팅해 상대와 겨루는 룰렛 승부
    ROULETTE_FEE, // 유저 -> 시스템, 룰렛 판당 수수료
    PACHINKO_FEE, // 유저 -> 시스템, 파칭코 판당 수수료
    PACHINKO, // 유저 <-> 시스템, 파칭코 결과 정산(순손익). amount는 시스템->유저(순이익) 또는 유저->시스템(순손실) 둘 다 가능
    SLOT_FEE, // 유저 -> 시스템, 슬롯머신 판당 수수료
    SLOT // 유저 <-> 시스템, 슬롯머신 결과 정산(순손익). amount는 시스템->유저(순이익) 또는 유저->시스템(순손실) 둘 다 가능
}
