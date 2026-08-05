package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.ActivityBlock;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BlockStatusResponse(
        boolean blocked,
        String reason,          // "OO님이 [24시간 차단권]을 사용해 차단했습니다" 형태의 조합 텍스트
        String blockerNickname,
        String itemName,
        LocalDateTime expiresAt,
        String unblockItemName, // 해제에 사용할 상품명 (= 차단에 쓰인 상품과 동일)
        Long unblockPrice       // 해제에 필요한 포인트
) {
    public static BlockStatusResponse notBlocked() {
        return BlockStatusResponse.builder().blocked(false).build();
    }

    public static BlockStatusResponse from(ActivityBlock block) {
        String blockerNickname = block.getBlocker().getDisplayNickname();
        // item이 null이면 구매한 차단이 아니라 ADMIN이 관리자 페이지에서 건 정지가 이 차단으로
        // 대체된 것이다 (ActivityBlock.item 문서 참고).
        boolean isAdminSuspend = block.getItem() == null;
        String itemName = isAdminSuspend ? "관리자 정지" : block.getItem().getName();
        String reason = isAdminSuspend
                ? blockerNickname + "님이 관리자 권한으로 활동을 정지했습니다."
                : blockerNickname + "님이 [" + itemName + "]을(를) 사용해 활동을 차단했습니다.";
        return BlockStatusResponse.builder()
                .blocked(true)
                .reason(reason)
                .blockerNickname(blockerNickname)
                .itemName(itemName)
                .expiresAt(block.getExpiresAt())
                .unblockItemName(itemName)
                // ShopItem.price(현재가)가 아니라 ActivityBlock.price(구매/연장 시점 스냅샷)를 써야
                // ActivityBlockService.unblockSelf()가 실제로 청구하는 금액과 일치한다. ROOT가 그 사이
                // 상품 가격을 바꿔도 이미 결제된 차단의 해제가는 바뀌지 않는다.
                .unblockPrice(block.getPrice())
                .build();
    }
}
