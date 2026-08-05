package com.jpkocommunity.domain.settings.repository;

import com.jpkocommunity.domain.settings.entity.SuspendUnlockSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuspendUnlockSettingRepository extends JpaRepository<SuspendUnlockSetting, Long> {

    // 싱글턴 테이블 — 항상 첫 행(유일한 행)을 가져온다.
    Optional<SuspendUnlockSetting> findFirstByOrderByIdAsc();
}
