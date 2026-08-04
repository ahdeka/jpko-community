package com.jpkocommunity.domain.settings.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 유저 정지(SuspendUserModal) 화면에 보여줄 기한 프리셋. ROOT가 직접 구성한다.
// 과거 정지 기록이 이 row를 참조하지 않는 단순 프리셋 목록이라 수정 없이 삭제/재생성만 지원한다.
// 이름·표시 순서는 별도로 두지 않는다 — 프론트가 minutes 값으로 "1시간"/"3일" 같은 라벨을 계산해서
// 보여주고, 항상 minutes 오름차순(짧은 것 → 긴 것)으로 정렬한다.
@Getter
@Entity
@Table(name = "suspend_duration_presets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuspendDurationPreset extends BaseEntity {

    @Column(nullable = false)
    private Long minutes;

    @Builder
    public SuspendDurationPreset(Long minutes) {
        this.minutes = minutes;
    }
}
