package com.jpkocommunity.domain.comment.repository;

import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.comment.entity.CommentTargetType;
import com.jpkocommunity.domain.user.dto.response.MyCommentResponse;
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

    // 최상위 댓글만 페이징 조회 (대댓글은 @BatchSize로 묶음 로딩)
    @Query("SELECT c FROM Comment c " +
            "WHERE c.targetType = :type AND c.targetId = :targetId AND c.parent IS NULL " +
            "ORDER BY c.createdAt ASC, c.id ASC")
    Page<Comment> findTopLevelByTarget(@Param("type") CommentTargetType type,
                                       @Param("targetId") Long targetId,
                                       Pageable pageable);

    // 게시글 목록 댓글 수 (POST 전용)
    @Query("SELECT c.targetId AS postId, COUNT(c) AS commentCount FROM Comment c " +
            "WHERE c.targetType = com.jpkocommunity.domain.comment.entity.CommentTargetType.POST " +
            "AND c.targetId IN :postIds " +
            "AND c.deletedAt IS NULL " +
            "GROUP BY c.targetId")
    List<PostCommentCount> countByPostIdIn(@Param("postIds") List<Long> postIds);

    // 헤더용 댓글 수 조회
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.targetType = :type AND c.targetId = :targetId " +
            "AND c.deletedAt IS NULL")
    long countByTarget(@Param("type") CommentTargetType type,
                       @Param("targetId") Long targetId);

    // 내 댓글 조회
    @Query("SELECT new com.jpkocommunity.domain.user.dto.response.MyCommentResponse(" +
            "c.id, p.id, p.title, c.content, c.createdAt) " +
            "FROM Comment c, Post p " +
            "WHERE c.targetType = com.jpkocommunity.domain.comment.entity.CommentTargetType.POST " +
            "AND c.targetId = p.id " +
            "AND c.user.id = :userId AND c.deletedAt IS NULL AND p.deletedAt IS NULL")
    Page<MyCommentResponse> findMyPostComments(@Param("userId") Long userId, Pageable pageable);

    // 신고 목록/집계 미리보기용 - id 목록으로 작성자·소속 게시글까지 함께 조회 (소프트 삭제 포함)
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.id IN :ids")
    List<Comment> findAllWithUserByIdIn(@Param("ids") List<Long> ids);

    // 공지 하드삭제 CASCADE
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.targetType = :type AND c.targetId = :targetId")
    void deleteByTargetTypeAndTargetId(@Param("type") CommentTargetType type, @Param("targetId") Long targetId);

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