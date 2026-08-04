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
        String itemName = block.getItem().getName();
        return BlockStatusResponse.builder()
                .blocked(true)
                .reason(blockerNickname + "님이 [" + itemName + "]을(를) 사용해 활동을 차단했습니다.")
                .blockerNickname(blockerNickname)
                .itemName(itemName)
                .expiresAt(block.getExpiresAt())
                .unblockItemName(itemName)
                .unblockPrice(block.getItem().getPrice())
                .build();
    }
}
