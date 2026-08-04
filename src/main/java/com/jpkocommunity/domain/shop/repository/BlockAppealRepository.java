package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.BlockAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockAppealRepository extends JpaRepository<BlockAppeal, Long> {

    Page<BlockAppeal> findByHandled(boolean handled, Pageable pageable);
}
