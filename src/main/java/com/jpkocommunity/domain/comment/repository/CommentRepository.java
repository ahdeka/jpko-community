package com.jpkocommunity.domain.comment.repository;

import com.jpkocommunity.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글 + 대댓글을 한 번의 쿼리로 fetch
    // DISTINCT: LEFT JOIN FETCH 시 중복 row 방지
    @Query("SELECT DISTINCT c FROM Comment c " +
            "LEFT JOIN FETCH c.replies " +
            "WHERE c.post.id = :postId AND c.parent IS NULL " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelWithRepliesByPostId(@Param("postId") Long postId);
}