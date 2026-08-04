package com.jpkocommunity.domain.settings.repository;

import com.jpkocommunity.domain.settings.entity.SuspendDurationPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuspendDurationPresetRepository extends JpaRepository<SuspendDurationPreset, Long> {

    List<SuspendDurationPreset> findAllByOrderByDisplayOrderAsc();
}
