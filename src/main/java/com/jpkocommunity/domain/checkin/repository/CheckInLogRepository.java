package com.jpkocommunity.domain.checkin.repository;

import com.jpkocommunity.domain.checkin.entity.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CheckInLogRepository extends JpaRepository<CheckInLog, Long> {

    boolean existsByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    // 이달 누적 출석 횟수
    int countByUserIdAndCheckInDateBetween(Long userId, LocalDate from, LocalDate to);

    List<CheckInLog> findByUserIdAndCheckInDateBetweenOrderByCheckInDate(
            Long userId, LocalDate from, LocalDate to);
}
