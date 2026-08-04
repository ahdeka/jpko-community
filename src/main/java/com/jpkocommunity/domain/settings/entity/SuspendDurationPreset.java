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
@Getter
@Entity
@Table(name = "suspend_duration_presets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuspendDurationPreset extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false)
    private Long minutes;

    @Column(nullable = false)
    private Integer displayOrder;

    @Builder
    public SuspendDurationPreset(String label, Long minutes, Integer displayOrder) {
        this.label = label;
        this.minutes = minutes;
        this.displayOrder = displayOrder;
    }
}
