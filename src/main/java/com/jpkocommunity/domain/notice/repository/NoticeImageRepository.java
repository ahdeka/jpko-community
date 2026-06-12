package com.jpkocommunity.domain.notice.repository;

import com.jpkocommunity.domain.notice.entity.NoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    List<NoticeImage> findByNoticeIdOrderByDisplayOrderAsc(Long noticeId);
}