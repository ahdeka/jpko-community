package com.jpkocommunity.domain.like.repository;

import com.jpkocommunity.domain.like.entity.Like;
import com.jpkocommunity.domain.like.entity.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // 유저가 해당 게시글에 이미 누른 기록 조회
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    // COUNT 쿼리 방식 - 타입별 집계
    long countByPostIdAndType(Long postId, LikeType type);
}