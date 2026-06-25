package com.jpkocommunity.domain.image.service;

import com.jpkocommunity.domain.image.dto.ImageUploadResponse;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.infra.s3.S3ImageUploader;
import com.jpkocommunity.global.infra.s3.S3UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3ImageUploader s3ImageUploader;

    private static final int MAX_IMAGES_PER_CONTENT = 5;
    private static final String TEMP_PREFIX = "temp";
    private static final String POSTS_PREFIX = "posts";
    private static final String NOTICES_PREFIX = "notices";

    // content HTML에서 임시 이미지 URL 추출을 위한 패턴
    private static final Pattern TEMP_URL_PATTERN =
            Pattern.compile("https?://[^/]+/(temp/[^\"\\s>]+)");

    // ========== Post 용 ==========

    public String moveTempImagesToPost(String content, Long postId) {
        return moveTempImages(content, POSTS_PREFIX + "/" + postId);
    }

    public void deleteOrphanedImages(String oldContent, String newContent, Long postId) {
        deleteOrphanedImagesByFolder(oldContent, newContent, POSTS_PREFIX + "/" + postId);
    }

    // ========== Notice 용 ==========

    public String moveTempImagesToNotice(String content, Long noticeId) {
        return moveTempImages(content, NOTICES_PREFIX + "/" + noticeId);
    }

    public void deleteOrphanedImagesForNotice(String oldContent, String newContent, Long noticeId) {
        deleteOrphanedImagesByFolder(oldContent, newContent, NOTICES_PREFIX + "/" + noticeId);
    }

    public void deleteAllNoticeImages(String content, Long noticeId) {
        Set<String> keys = extractImageKeys(content, NOTICES_PREFIX + "/" + noticeId);
        if (keys.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(s3ImageUploader::delete);
            }
        });
    }

    // ========== HTML 처리 ==========

    // 저장 전 XSS 방어 + 이미지 수 제한 검증
    public String sanitizeAndValidate(String content) {
        String sanitized = sanitize(content);
        validateImageCount(sanitized);
        return sanitized;
    }

    // 목록에서 이미지 포함 여부 확인
    public boolean hasImage(String content) {
        if (content == null) {
            return false;
        }
        return Jsoup.parse(content).select("img").stream()
                .anyMatch(img -> StringUtils.hasText(img.attr("src")));
    }

    // ========== S3 업로드 ==========

    // 에디터에서 이미지 즉시 S3 업로드
    public ImageUploadResponse uploadTemp(MultipartFile file) {
        S3UploadResult result = s3ImageUploader.upload(file, TEMP_PREFIX);
        return new ImageUploadResponse(result.imageUrl());
    }

    // ========== private 메서드 ==========

    // HTML content 저장 전 악성 태그 제거 *XSS 방어
    private String sanitize(String html) {
        return Jsoup.clean(html, Safelist.relaxed());
    }

    private void validateImageCount(String content) {
        int count = Jsoup.parse(content).select("img").size();
        if (count > MAX_IMAGES_PER_CONTENT) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
    }

    private String moveTempImages(String content, String targetFolder) {
        Matcher matcher = TEMP_URL_PATTERN.matcher(content);
        List<String[]> replacements = new ArrayList<>(); // [원본 URL, 새 URL]
        List<String> copiedKeys = new ArrayList<>();     // 새로 생긴 키
        List<String> tempKeys = new ArrayList<>();       // 원본 temp/ 키

        while (matcher.find()) {
            String originalUrl = matcher.group(0);
            String tempKey = matcher.group(1);                          // "temp/uuid.jpg"
            String filename = tempKey.substring(TEMP_PREFIX.length() + 1); // "uuid.jpg"
            String newKey = targetFolder + "/" + filename;              // "posts/1/uuid.jpg"

            try {
                String newUrl = s3ImageUploader.copy(tempKey, newKey);
                replacements.add(new String[]{originalUrl, newUrl});
                copiedKeys.add(newKey);
                tempKeys.add(tempKey);
            } catch (Exception e) {
                log.error("S3 copy 실패 - tempKey: {}, targetFolder: {}", tempKey, targetFolder);
                compensate(copiedKeys);
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        // 여기 도달 = 모든 copy 성공
        String updatedContent = content;
        for (String[] r : replacements) {
            updatedContent = updatedContent.replace(r[0], r[1]);
        }

        if (!copiedKeys.isEmpty()) {
            registerMoveCleanup(copiedKeys, tempKeys);
        }

        return updatedContent;
    }

    // 고아 이미지 삭제
    private void deleteOrphanedImagesByFolder(String oldContent, String newContent, String folder) {
        Set<String> oldKeys = extractImageKeys(oldContent, folder);
        Set<String> newKeys = extractImageKeys(newContent, folder);
        oldKeys.removeAll(newKeys);

        if (oldKeys.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                oldKeys.forEach(s3ImageUploader::delete);
            }
        });
    }

    // 이미지 URL에서 S3 키 추출
    private Set<String> extractImageKeys(String content, String folder) {
        String prefix = folder + "/";
        return Jsoup.parse(content).select("img").stream()
                .map(img -> img.attr("src"))
                .map(s3ImageUploader::extractKeyIfOwned)
                .filter(Objects::nonNull)
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toSet());
    }

    private void compensate(List<String> copiedKeys) {
        copiedKeys.forEach(s3ImageUploader::delete);
    }

    private void registerMoveCleanup(List<String> copiedKeys, List<String> tempKeys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    // 커밋 성공 - temp 원본 삭제
                    tempKeys.forEach(key -> {
                        try {
                            s3ImageUploader.delete(key);
                        } catch (Exception e) {
                            log.warn("temp 파일 삭제 실패 (Lifecycle이 처리) - key: {}", key);
                        }
                    });
                } else {
                    // 롤백 - 새 복사본이 고아가 됨, 즉시 정리
                    copiedKeys.forEach(s3ImageUploader::delete);
                }
            }
        });
    }
}
