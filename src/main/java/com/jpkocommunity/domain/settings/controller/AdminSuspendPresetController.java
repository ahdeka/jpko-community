package com.jpkocommunity.domain.settings.controller;

import com.jpkocommunity.domain.settings.dto.request.CreateSuspendDurationPresetRequest;
import com.jpkocommunity.domain.settings.dto.response.SuspendDurationPresetResponse;
import com.jpkocommunity.domain.settings.service.SuspendSettingsService;
import com.jpkocommunity.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings/suspend-presets")
@RequiredArgsConstructor
public class AdminSuspendPresetController {

    private final SuspendSettingsService suspendSettingsService;

    // 정지 모달을 여는 어떤 관리자(ADMIN/ROOT)든 조회는 가능해야 한다.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<List<SuspendDurationPresetResponse>>> getPresets() {
        return ResponseEntity.ok(ApiResponse.ok(suspendSettingsService.getPresets()));
    }

    // 구성(추가/삭제)은 ROOT 전용.
    @PostMapping
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<ApiResponse<Void>> createPreset(@Valid @RequestBody CreateSuspendDurationPresetRequest request) {
        suspendSettingsService.createPreset(request);
        return ResponseEntity.ok(ApiResponse.ok("프리셋이 등록되었습니다."));
    }

    @DeleteMapping("/{presetId}")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<ApiResponse<Void>> deletePreset(@PathVariable Long presetId) {
        suspendSettingsService.deletePreset(presetId);
        return ResponseEntity.ok(ApiResponse.ok("프리셋이 삭제되었습니다."));
    }
}
