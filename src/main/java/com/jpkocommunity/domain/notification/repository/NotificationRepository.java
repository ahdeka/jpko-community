package com.jpkocommunity.domain.notification.repository;

import com.jpkocommunity.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // "안 읽은 알림 목록" 조회용 (N+1 방지를 위해 fetch join)
    @Query("""
            select n from Notification n
            join fetch n.sender
            left join fetch n.comment
            where n.receiver.id = :receiverId and n.isRead = false
            order by n.createdAt desc
            """)
    List<Notification> findUnreadWithDetailsByReceiverId(@Param("receiverId") Long receiverId);
}
