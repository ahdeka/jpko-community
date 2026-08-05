package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.RolePurchase;
import com.jpkocommunity.domain.shop.entity.RolePurchaseStatus;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RolePurchaseRepository extends JpaRepository<RolePurchase, Long> {

    Optional<RolePurchase> findByUserAndStatus(User user, RolePurchaseStatus status);

    boolean existsByUser_IdAndStatusAndTargetRole(Long userId, RolePurchaseStatus status, UserRole targetRole);

    Page<RolePurchase> findByStatus(RolePurchaseStatus status, Pageable pageable);

    @Query("select rp from RolePurchase rp where rp.status = 'ACTIVE' and rp.expiresAt <= :now")
    List<RolePurchase> findAllExpired(@Param("now") LocalDateTime now);
}
