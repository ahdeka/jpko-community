package com.jpkocommunity.domain.settings.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 정지(SUSPENDED)를 본인이 포인트로 즉시 해제할 때의 가격. ROOT가 설정한다.
// 항상 정확히 1행만 존재하는 싱글턴 — SuspendSettingsService가 첫 행을 그대로 쓰고 갱신만 한다.
@Getter
@Entity
@Table(name = "suspend_unlock_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuspendUnlockSetting extends BaseEntity {

    @Column(nullable = false)
    private Long price;

    public void updatePrice(Long price) {
        this.price = price;
    }
}
