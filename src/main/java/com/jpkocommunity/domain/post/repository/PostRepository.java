package com.jpkocommunity.domain.post.repository;

import com.jpkocommunity.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // deletedAt IS NULL 조건으로 삭제된 게시글 필터링
    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId AND p.deletedAt IS NULL")
    Page<Post> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // 전체 게시글 목록 (삭제 제외)
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL")
    Page<Post> findAllActive(Pageable pageable);
}