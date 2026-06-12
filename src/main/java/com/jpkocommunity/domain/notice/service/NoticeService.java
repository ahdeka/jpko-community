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
import com.jpkocommunity.global.infra.s3.S3ImageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserService userService;
    private final S3ImageUploader s3ImageUploader;

    public Page<NoticeSummaryResponse> getNotices(Pageable pageable) {
        return noticeRepository.findAll(pageable)
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
        Notice notice = findById(noticeId);
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
        Notice notice = findById(noticeId);

        notice.update(request.title(), request.content(), request.pinned());

        return NoticeResponse.from(notice.getId());
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = findById(noticeId);

        List<String> s3Keys = notice.getImages().stream()
                .map(image -> image.getS3Key())
                .toList();

        noticeRepository.delete(notice);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3Keys.forEach(s3ImageUploader::delete);
            }
        });
    }

    // ========== private 메서드 ==========

    Notice findById(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
    }
}
