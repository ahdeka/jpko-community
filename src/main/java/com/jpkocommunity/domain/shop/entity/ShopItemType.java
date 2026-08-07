package com.jpkocommunity.domain.shop.entity;

public enum ShopItemType {
    BLOCK,        // 정가 판매, 임시 활동 차단권
    ROLE_AUCTION, // 주기적으로 경매가 열리는 UserRole 판매 설정
    SHIELD        // 정가 판매, 룰렛 대결 대상에서 임시로 빠지는 방어권
}
