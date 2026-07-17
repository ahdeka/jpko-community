package com.jpkocommunity.domain.comment.repository;

import com.jpkocommunity.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글 + 대댓글을 한 번의 쿼리로 fetch
    // DISTINCT: LEFT JOIN FETCH 시 중복 row 방지
    @Query("SELECT DISTINCT c FROM Comment c " +
            "LEFT JOIN FETCH c.replies " +
            "WHERE c.post.id = :postId AND c.parent IS NULL " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelWithRepliesByPostId(@Param("postId") Long postId);

    // 게시글 목록용 - 게시글별 댓글 수를 한 번의 쿼리로 집계
    @Query("SELECT c.post.id AS postId, COUNT(c) AS commentCount FROM Comment c " +
            "WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<PostCommentCount> countByPostIdIn(@Param("postIds") List<Long> postIds);

    // 사용자별 댓글 조회 (삭제 제외)
    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user.id = :userId AND c.deletedAt IS NULL AND c.post.deletedAt IS NULL")
    Page<Comment> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);

    // 신고 목록/집계 미리보기용 - id 목록으로 작성자·소속 게시글까지 함께 조회 (소프트 삭제 포함)
    @Query("SELECT c FROM Comment c JOIN FETCH c.user JOIN FETCH c.post WHERE c.id IN :ids")
    List<Comment> findAllWithUserByIdIn(@Param("ids") List<Long> ids);

    // IP 주소 익명화
    @Modifying
    @Query("UPDATE Comment c SET c.ipAddress = NULL WHERE c.ipAddress IS NOT NULL AND c.createdAt < :cutoff")
    int anonymizeIpBefore(@Param("cutoff") LocalDateTime cutoff);

    // 도배 방지용 - 유저의 마지막 댓글 작성 시간만 조회
    @Query(value = "SELECT c.createdAt FROM Comment c WHERE c.user.id = :userId ORDER BY c.createdAt DESC LIMIT 1")
    Optional<LocalDateTime> findLatestCreatedAtByUserId(@Param("userId") Long userId);

    // 도배 방지용 - 유저 단위 락 (classifier로 Post 스로틀링 락과 네임스페이스 분리)
    @Query(value = "SELECT pg_advisory_xact_lock(:classifier, CAST(:userId AS integer))", nativeQuery = true)
    void acquireUserLock(@Param("classifier") int classifier, @Param("userId") Long userId);
}