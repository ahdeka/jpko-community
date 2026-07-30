package com.jpkocommunity.domain.point.repository;

import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {

    Optional<Point> findByUser(User user);

    Optional<Point> findByUserIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p where p.user = :user")
    Optional<Point> findByUserForUpdate(@Param("user") User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p where p.user is null")
    Optional<Point> findSystemAccountForUpdate();
}