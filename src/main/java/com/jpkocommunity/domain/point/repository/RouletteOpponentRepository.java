package com.jpkocommunity.domain.point.repository;

import com.jpkocommunity.domain.point.entity.RouletteOpponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouletteOpponentRepository extends JpaRepository<RouletteOpponent, Long> {

    Optional<RouletteOpponent> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
