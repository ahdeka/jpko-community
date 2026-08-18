package com.jpkocommunity.domain.notification.repository;

import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // "안 읽은 알림 목록" 조회용 (N+1 방지를 위해 fetch join)
    // POINT_STOLEN처럼 post가 없는 알림 /  POST처럼 sender가 없는 알림도 있어 left join fetch (inner join이면 해당 알림이 통째로 누락됨)
    @Query("""
            select n from Notification n
            left join fetch n.sender
            left join fetch n.post
            left join fetch n.comment
            where n.receiver.id = :receiverId and n.isRead = false
            order by n.createdAt desc
            """)
    List<Notification> findUnreadWithDetailsByReceiverId(@Param("receiverId") Long receiverId);

    // 전체 읽음 처리 - 벌크 UPDATE 한방
    @Modifying
    @Query("update Notification n set n.isRead = true where n.receiver.id = :userId and n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    // 전체 삭제
    @Modifying
    @Query("delete from Notification n where n.receiver.id = :userId")
    void deleteAllByReceiverId(@Param("userId") Long userId);

    // 알림 목록 조회
    @Query(value = """
            select n from Notification n
            left join fetch n.sender
            left join fetch n.post
            left join fetch n.comment
            where n.receiver.id = :userId
            """,
            countQuery = "select count(n) from Notification n where n.receiver.id = :userId")
    Page<Notification> findAllByReceiverId(@Param("userId") Long userId, Pageable pageable);

    // 보관기간 지난 알림 정리
    @Modifying
    @Query("delete from Notification n where n.createdAt < :cutoff")
    int deleteAllOlderThan(@Param("cutoff") LocalDateTime cutoff);

    // 댓글 하드 삭제 시, 그 댓글을 참조하는 알림을 먼저 끊기 위한 벌크 삭제
    @Modifying
    @Query("delete from Notification n where n.comment.id in :commentIds")
    void deleteByCommentIdIn(@Param("commentIds") List<Long> commentIds);

    // 좋아요 재클릭 중복 알림 억제용
    Optional<Notification> findTopByReceiverIdAndSenderIdAndPostIdAndTypeOrderByCreatedAtDesc(
            Long receiverId, Long senderId, Long postId, NotificationType type
    );
}
