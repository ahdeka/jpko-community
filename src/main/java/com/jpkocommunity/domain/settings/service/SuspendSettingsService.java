package com.jpkocommunity.domain.settings.service;

import com.jpkocommunity.domain.settings.dto.request.CreateSuspendDurationPresetRequest;
import com.jpkocommunity.domain.settings.dto.response.SuspendDurationPresetResponse;
import com.jpkocommunity.domain.settings.entity.SuspendDurationPreset;
import com.jpkocommunity.domain.settings.repository.SuspendDurationPresetRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 유저 정지 기한 프리셋 관리. 조회는 정지 모달을 여는 어떤 관리자든 필요하지만,
// 구성(추가/삭제)은 ROOT 전용 — AdminSuspendPresetController의 @PreAuthorize에서 강제한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuspendSettingsService {

    private final SuspendDurationPresetRepository presetRepository;

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
}
