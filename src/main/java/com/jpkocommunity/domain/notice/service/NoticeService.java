package com.jpkocommunity.domain.notice.service;

import com.jpkocommunity.domain.notice.dto.request.NoticeCreateRequest;
import com.jpkocommunity.domain.notice.dto.request.NoticeUpdateRequest;
import com.jpkocommunity.domain.notice.dto.response.NoticeDetailResponse;
import com.jpkocommunity.domain.notice.dto.response.NoticeResponse;
import com.jpkocommunity.domain.notice.dto.response.NoticeSummaryResponse;
import com.jpkocommunity.domain.notice.entity.Notice;
import com.jpkocommunity.domain.notice.repository.NoticeRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserService userService;

    public Page<NoticeSummaryResponse> getNotices(Pageable pageable) {
        return noticeRepository.findAllActive(pageable)
                .map(NoticeSummaryResponse::from);
    }

    // 게시판 목록 상단에 고정 노출할 pinned 공지 목록
    public List<NoticeSummaryResponse> getPinnedNotices() {
        return noticeRepository.findAllPinned().stream()
                .map(NoticeSummaryResponse::from)
                .toList();
    }

    @Transactional
    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = findActiveById(noticeId);
        notice.increaseViewCount();
        return NoticeDetailResponse.from(notice);
    }

    @Transactional
    public NoticeResponse createNotice(Long userId, NoticeCreateRequest request) {
        User user = userService.findById(userId);

        Notice notice = Notice.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .pinned(request.pinned())
                .build();

        noticeRepository.save(notice);

        return NoticeResponse.from(notice.getId());
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = findActiveById(noticeId);

        notice.update(request.title(), request.content(), request.pinned());

        return NoticeResponse.from(notice.getId());
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = findActiveById(noticeId);
        notice.delete();
    }

    // ========== private 메서드 ==========

    Notice findActiveById(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        if (notice.isDeleted()) {
            throw new CustomException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }
}
