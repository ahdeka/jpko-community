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
    // post는 nullable=false라 inner join fetch로 안전하게 제목까지 함께 로드
    @Query("""
            select n from Notification n
            join fetch n.sender
            join fetch n.post
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
