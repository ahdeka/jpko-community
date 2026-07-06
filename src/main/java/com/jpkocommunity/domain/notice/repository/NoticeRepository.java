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

    // 조회수 +1 (벌크 UPDATE).
    // 엔티티 필드를 직접 수정하면 @LastModifiedDate가 걸려 updated_at까지 갱신되므로,
    // 조회만으로 updated_at이 오염되지 않도록 감사 리스너를 우회하는 벌크 JPQL을 사용한다.
    // DB에서 원자적으로 +1 되므로 동시 조회 시 증가분 유실(lost update)도 방지된다.
    @Modifying
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
