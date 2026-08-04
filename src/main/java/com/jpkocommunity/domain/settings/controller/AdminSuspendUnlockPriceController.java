package com.jpkocommunity.domain.settings.controller;

import com.jpkocommunity.domain.settings.dto.request.UpdateSuspendUnlockPriceRequest;
import com.jpkocommunity.domain.settings.dto.response.SuspendUnlockPriceResponse;
import com.jpkocommunity.domain.settings.service.SuspendSettingsService;
import com.jpkocommunity.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// 관리자 정지(SUSPENDED)를 포인트로 즉시 해제할 때의 가격 설정. 조회/변경 모두 ROOT 전용 —
// 이 가격을 정하는 것도 사실상 "정지 정책"이라 ADMIN에게는 노출하지 않는다.
@RestController
@RequestMapping("/api/admin/settings/suspend-unlock-price")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROOT')")
public class AdminSuspendUnlockPriceController {

    private final SuspendSettingsService suspendSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<SuspendUnlockPriceResponse>> getPrice() {
        return ResponseEntity.ok(ApiResponse.ok(new SuspendUnlockPriceResponse(suspendSettingsService.getUnlockPrice())));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updatePrice(@Valid @RequestBody UpdateSuspendUnlockPriceRequest request) {
        suspendSettingsService.updateUnlockPrice(request.price());
        return ResponseEntity.ok(ApiResponse.ok("가격이 변경되었습니다."));
    }
}
