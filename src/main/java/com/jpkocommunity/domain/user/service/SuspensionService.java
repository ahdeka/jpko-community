package com.jpkocommunity.domain.user.service;

import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.domain.settings.service.SuspendSettingsService;
import com.jpkocommunity.domain.user.dto.response.SuspensionStatusResponse;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 정지(SUSPENDED)를 본인이 포인트로 즉시 해제하는 자기 서비스.
// UserService가 아니라 별도 서비스로 둔 이유: PointService.charge/lockAccount가 내부적으로
// UserService.findById를 호출하므로, 이 로직을 UserService에 넣고 PointService를 주입받으면
// UserService <-> PointService 순환 의존이 생긴다. ActivityBlockService(활동 차단 자기 해제)와
// 같은 이유로 별도 서비스에서 양쪽을 조합한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuspensionService {

    private final UserService userService;
    private final PointService pointService;
    private final SuspendSettingsService suspendSettingsService;

    public SuspensionStatusResponse getMyStatus(Long userId) {
        User user = userService.findById(userId);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            return SuspensionStatusResponse.notSuspended();
        }
        return SuspensionStatusResponse.suspended(user.getSuspendedUntil(), suspendSettingsService.getUnlockPrice());
    }

    @Transactional
    public void unsuspendSelf(Long userId) {
        User user = userService.findById(userId);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            // 1분 주기 스케줄러가 그 사이 기한부 정지를 자동 해제했을 수 있다(경쟁 상황) — 정상 케이스.
            throw new CustomException(ErrorCode.NOT_SUSPENDED);
        }

        Long price = suspendSettingsService.getUnlockPrice();

        // charge()보다 activate()를 먼저 호출한다 — PointService.charge → lockAccount → validateActive가
        // SUSPENDED 상태면 결제 자체를 막는다(같은 트랜잭션·영속성 컨텍스트라 activate()가 flush 전이어도
        // 이 검증엔 바로 반영된다). charge가 잔액 부족 등으로 실패하면 트랜잭션 전체가 롤백되어 activate()도
        // 함께 취소되므로 "돈은 안 냈는데 풀렸다" 상태가 될 수 없다.
        user.activate();
        pointService.charge(userId, price, TransactionType.SUSPEND_UNLOCK, "관리자 정지 포인트 즉시 해제");
    }
}
