package com.jpkocommunity.domain.notice.service;

import com.jpkocommunity.domain.image.service.ImageService;
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
    private final ImageService imageService;

    public Page<NoticeSummaryResponse> getNotices(Pageable pageable) {
        return noticeRepository.findAll(pageable)
                .map(NoticeSummaryResponse::from);
    }

    // 게시판 목록 상단 고정용
    public List<NoticeSummaryResponse> getPinnedNotices() {
        return noticeRepository.findAllPinned().stream()
                .map(NoticeSummaryResponse::from)
                .toList();
    }

    // 메인 상단 중요 공지용
    public List<NoticeSummaryResponse> getFeaturedNotices() {
        return noticeRepository.findAllFeatured().stream()
                .map(NoticeSummaryResponse::from)
                .toList();
    }

    @Transactional
    public NoticeDetailResponse getNotice(Long noticeId, boolean shouldIncreaseView) {
        Notice notice = findById(noticeId);

        int viewCount = notice.getViewCount();
        if (shouldIncreaseView) {
            noticeRepository.incrementViewCount(noticeId);
            viewCount++;
        }

        return NoticeDetailResponse.from(notice, viewCount);
    }

    @Transactional
    public NoticeResponse createNotice(Long userId, NoticeCreateRequest request) {
        User user = userService.findById(userId);

        String sanitizedContent = imageService.sanitizeAndValidate(request.content());

        Notice notice = noticeRepository.save(Notice.builder()
                .user(user)
                .title(request.title())
                .content(sanitizedContent)
                .pinned(request.pinned())
                .featured(request.featured())
                .build());

        String movedContent = imageService.moveTempImagesToNotice(sanitizedContent, notice.getId());
        notice.confirmContent(movedContent);

        return NoticeResponse.from(notice.getId());
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = findById(noticeId);

        String oldContent = notice.getContent();
        String sanitizedContent = imageService.sanitizeAndValidate(request.content());
        String movedContent = imageService.moveTempImagesToNotice(sanitizedContent, noticeId);
        imageService.deleteOrphanedImagesForNotice(oldContent, movedContent, noticeId);

        notice.update(request.title(), movedContent, request.pinned(), request.featured());

        return NoticeResponse.from(notice.getId());
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = findById(noticeId);
        imageService.deleteAllNoticeImages(notice.getContent(), noticeId);
        noticeRepository.delete(notice);
    }

    // ========== private 메서드 ==========

    Notice findById(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
    }
}
