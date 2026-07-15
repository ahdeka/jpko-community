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

    // 신고 목록/집계 미리보기용 - id 목록으로 작성자까지 함께 조회 (소프트 삭제 포함)
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.id IN :ids")
    List<Comment> findAllWithUserByIdIn(@Param("ids") List<Long> ids);

    // IP 주소 익명화
    @Modifying
    @Query("UPDATE Comment c SET c.ipAddress = NULL WHERE c.ipAddress IS NOT NULL AND c.createdAt < :cutoff")
    int anonymizeIpBefore(@Param("cutoff") LocalDateTime cutoff);
}