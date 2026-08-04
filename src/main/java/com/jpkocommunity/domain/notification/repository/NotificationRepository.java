package com.jpkocommunity.domain.notification.repository;

import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 좋아요 재클릭 중복 알림 억제용
    Optional<Notification> findTopByReceiverIdAndSenderIdAndPostIdAndTypeOrderByCreatedAtDesc(
            Long receiverId, Long senderId, Long postId, NotificationType type
    );
}
