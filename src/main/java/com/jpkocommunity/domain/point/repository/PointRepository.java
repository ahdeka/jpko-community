package com.jpkocommunity.domain.point.repository;

import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByUser(User user);
}
