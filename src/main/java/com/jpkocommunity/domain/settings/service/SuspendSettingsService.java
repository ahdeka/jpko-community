package com.jpkocommunity.domain.settings.service;

import com.jpkocommunity.domain.settings.dto.request.CreateSuspendDurationPresetRequest;
import com.jpkocommunity.domain.settings.dto.response.SuspendDurationPresetResponse;
import com.jpkocommunity.domain.settings.entity.SuspendDurationPreset;
import com.jpkocommunity.domain.settings.entity.SuspendUnlockSetting;
import com.jpkocommunity.domain.settings.repository.SuspendDurationPresetRepository;
import com.jpkocommunity.domain.settings.repository.SuspendUnlockSettingRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 유저 정지 관련 설정. 프리셋 조회/정지 해제가격 조회는 정지 모달·화면을 여는 어떤 관리자든
// (또는 정지된 본인) 필요하지만, 구성(추가/삭제/가격 변경)은 ROOT 전용 —
// AdminSuspendPresetController/AdminSuspendUnlockPriceController의 @PreAuthorize에서 강제한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuspendSettingsService {

    private final SuspendDurationPresetRepository presetRepository;
    private final SuspendUnlockSettingRepository unlockSettingRepository;

    public List<SuspendDurationPresetResponse> getPresets() {
        return presetRepository.findAllByOrderByMinutesAsc().stream()
                .map(SuspendDurationPresetResponse::from)
                .toList();
    }

    @Transactional
    public void createPreset(CreateSuspendDurationPresetRequest request) {
        presetRepository.save(SuspendDurationPreset.builder()
                .minutes(request.minutes())
                .build());
    }

    @Transactional
    public void deletePreset(Long presetId) {
        if (!presetRepository.existsById(presetId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        presetRepository.deleteById(presetId);
    }

    public Long getUnlockPrice() {
        return getUnlockSettingRow().getPrice();
    }

    @Transactional
    public void updateUnlockPrice(Long price) {
        getUnlockSettingRow().updatePrice(price);
    }

    // 싱글턴 행. 마이그레이션이 항상 1행을 시드하므로 실제로는 비어 있을 수 없지만,
    // 방어적으로 명확한 에러를 던진다.
    private SuspendUnlockSetting getUnlockSettingRow() {
        return unlockSettingRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
