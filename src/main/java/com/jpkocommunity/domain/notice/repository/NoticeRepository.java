package com.jpkocommunity.domain.notice.repository;

import com.jpkocommunity.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 게시글 목록 상단 고정
    @Query("SELECT n FROM Notice n WHERE n.pinned = true ORDER BY n.createdAt DESC")
    List<Notice> findAllPinned();

    // 메인 상단 중요 공지
    @Query("SELECT n FROM Notice n WHERE n.featured = true ORDER BY n.createdAt DESC")
    List<Notice> findAllFeatured();

    // 조회수 +1 (벌크 UPDATE)
    @Modifying
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
