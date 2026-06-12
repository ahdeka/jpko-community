package com.jpkocommunity.domain.notice.repository;

import com.jpkocommunity.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 다른 게시판 목록 상단에 붙일 pinned 공지 목록
    // pinned 공지는 소량이므로 Page 없이 List로 조회
    @Query("SELECT n FROM Notice n WHERE n.pinned = true ORDER BY n.createdAt DESC")
    List<Notice> findAllPinned();
}
