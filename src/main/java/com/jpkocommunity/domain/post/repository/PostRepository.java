package com.jpkocommunity.domain.post.repository;

import com.jpkocommunity.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // deletedAt IS NULL 조건으로 삭제된 게시글 필터링
    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId AND p.deletedAt IS NULL")
    Page<Post> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // 전체 게시글 목록 (삭제 제외)
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL")
    Page<Post> findAllActive(Pageable pageable);

    // 제목 검색 (categoryId가 null이면 전체 카테고리 대상)
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> searchByTitle(@Param("categoryId") Long categoryId,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    // 제목 + 내용 검색
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchByTitleAndContent(@Param("categoryId") Long categoryId,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    // 인기글: since 이후 작성된 글을 좋아요 수 → 조회수 → 최신순으로 정렬
    // LEFT JOIN: 좋아요 0개인 글도 결과에 포함시키기 위함
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN Like l ON l.post = p AND l.type = com.jpkocommunity.domain.like.entity.LikeType.LIKE " +
            "WHERE p.deletedAt IS NULL AND p.createdAt >= :since " +
            "GROUP BY p " +
            "ORDER BY COUNT(l) DESC, p.viewCount DESC, p.createdAt DESC")
    List<Post> findPopularPosts(@Param("since") LocalDateTime since, Pageable pageable);
}