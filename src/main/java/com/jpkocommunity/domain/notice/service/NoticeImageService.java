package com.jpkocommunity.domain.notice.service;

import com.jpkocommunity.domain.notice.dto.response.NoticeImageResponse;
import com.jpkocommunity.domain.notice.entity.Notice;
import com.jpkocommunity.domain.notice.entity.NoticeImage;
import com.jpkocommunity.domain.notice.repository.NoticeImageRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.infra.s3.S3ImageUploader;
import com.jpkocommunity.global.infra.s3.S3UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeImageService {

    private static final int MAX_IMAGES_PER_NOTICE = 5;

    private final S3ImageUploader s3ImageUploader;
    private final NoticeService noticeService;
    private final NoticeImageRepository noticeImageRepository;

    @Transactional
    public NoticeImageResponse upload(Long noticeId, MultipartFile file, int displayOrder) {
        Notice notice = noticeService.findById(noticeId);

        if (noticeImageRepository.findByNoticeIdOrderByDisplayOrderAsc(noticeId).size() >= MAX_IMAGES_PER_NOTICE) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        String prefix = "notices/" + noticeId;
        S3UploadResult result = s3ImageUploader.upload(file, prefix);

        try {
            NoticeImage saved = noticeImageRepository.save(NoticeImage.builder()
                    .notice(notice)
                    .s3Key(result.s3Key())
                    .imageUrl(result.imageUrl())
                    .displayOrder(displayOrder)
                    .build());
            return NoticeImageResponse.from(saved);
        } catch (Exception e) {
            log.error("DB 저장 실패, S3 보상 삭제 - key: {}", result.s3Key(), e);
            s3ImageUploader.delete(result.s3Key());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public List<NoticeImageResponse> getImages(Long noticeId) {
        return noticeImageRepository.findByNoticeIdOrderByDisplayOrderAsc(noticeId).stream()
                .map(NoticeImageResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long noticeId, Long imageId) {
        // 공지 존재 여부 확인
        noticeService.findById(noticeId);

        NoticeImage noticeImage = noticeImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        if (!noticeImage.getNotice().getId().equals(noticeId)) {
            throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String s3Key = noticeImage.getS3Key();
        noticeImageRepository.delete(noticeImage);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3ImageUploader.delete(s3Key);
            }
        });
    }

}
