package com.jpkocommunity.domain.post.repository;

import com.jpkocommunity.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // 닉네임 검색 - 정확 일치
    @Query("SELECT p FROM Post p JOIN p.user u WHERE p.deletedAt IS NULL " +
            "AND p.anonymous = false " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND LOWER(u.nickname) = LOWER(:keyword)")
    Page<Post> searchByNickname(@Param("categoryId") Long categoryId,
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

    // 사용자별 게시글 조회 (삭제 제외)
    @Query("SELECT p FROM Post p JOIN FETCH p.category WHERE p.user.id = :userId AND p.deletedAt IS NULL")
    Page<Post> findByUserIdWithCategory(@Param("userId") Long userId, Pageable pageable);

    // 신고 목록/집계 미리보기용 - id 목록으로 작성자까지 함께 조회 (소프트 삭제 포함)
    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id IN :ids")
    List<Post> findAllWithUserByIdIn(@Param("ids") List<Long> ids);

    // 조회수 증가를 위한 벌크 업데이트
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // IP 주소 익명화
    @Modifying
    @Query("UPDATE Post p SET p.ipAddress = NULL WHERE p.ipAddress IS NOT NULL AND p.createdAt < :cutoff")
    int anonymizeIpBefore(@Param("cutoff") LocalDateTime cutoff);

    // 도배 방지용 - 유저의 마지막 게시글 작성 시간만 조회
    @Query("SELECT p.createdAt FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC LIMIT 1")
    Optional<LocalDateTime> findLatestCreatedAtByUserId(@Param("userId") Long userId);

    // 도배 방지용 - 유저 단위 락 (classifier로 Post 스로틀링 락과 네임스페이스 분리)
    @Query(value = "SELECT pg_advisory_xact_lock(:classifier, CAST(:userId AS integer))", nativeQuery = true)
    void acquireUserLock(@Param("classifier") int classifier, @Param("userId") Long userId);

}