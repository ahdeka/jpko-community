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

    private static final int MAX_IMAGES_PER_POST = 5;
    private static final String TEMP_PREFIX = "temp";
    private static final String POSTS_PREFIX = "posts";

    // content HTML에서 임시 이미지 URL 추출을 위한 패턴
    private static final Pattern TEMP_URL_PATTERN =
            Pattern.compile("https?://[^/]+/(temp/[^\"\\s>]+)");

    // 에디터에서 이미지 즉시 S3 업로드
    public ImageUploadResponse uploadTemp(MultipartFile file) {
        S3UploadResult result = s3ImageUploader.upload(file, TEMP_PREFIX);
        return new ImageUploadResponse(result.imageUrl());
    }

    // 게시글 저장 및 수정 시 임시 이미지들을 posts/{postId}로 이동
    public String moveTempImagesToPost(String content, Long postId) {
        Matcher matcher = TEMP_URL_PATTERN.matcher(content);
        List<String[]> replacements = new ArrayList<>(); // [원본 URL, 새 URL]
        List<String> copiedKeys = new ArrayList<>(); // posts/{postId}/ 에 새로 생긴 키
        List<String> tempKeys = new ArrayList<>(); // 원본 temp/ 키

        while (matcher.find()) {
            String originalUrl = matcher.group(0);           // 전체 URL
            String tempKey = matcher.group(1);               // "temp/uuid.jpg"
            String filename = tempKey.substring(TEMP_PREFIX.length() + 1); // "uuid.jpg"
            String newKey = POSTS_PREFIX + "/" + postId + "/" + filename; // "posts/1/uuid.jpg"

            try {
                String newUrl = s3ImageUploader.copy(tempKey, newKey);
                replacements.add(new String[]{originalUrl, newUrl, tempKey});
                copiedKeys.add(newKey);
                tempKeys.add(tempKey);
            } catch (Exception e) {
                log.error("S3 copy 실패 - tempKey: {}, postId: {}", tempKey, postId);
                compensate(copiedKeys); // 지금까지 성공한 copy를 보상 처리 (삭제)
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

    // HTML content 저장 전 악성 태그 제거 -> 안전한 HTML만 DB에 저장 *XSS 방어
    public String sanitize(String html) {
        return Jsoup.clean(html, Safelist.relaxed());
    }

    // 게시글당 이미지 수 제한 검증
    public void validateImageCount(String content) {
        int count = Jsoup.parse(content).select("img").size();
        if (count > MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
    }

    // 게시글 수정 시 본문에서 삭제된 이미지를 S3에서 정리 (수정 전/후 content 비교)
    public void deleteOrphanedImages(String oldContent, String newContent, Long postId) {
        Set<String> oldKeys = extractPostImageKeys(oldContent, postId);
        Set<String> newKeys = extractPostImageKeys(newContent, postId);
        oldKeys.removeAll(newKeys); // 제거된 키만 남음

        if (oldKeys.isEmpty()) {
            return;
        }

        // 커밋 성공 후에만 실제 S3 삭제 - 트랜잭션 롤백 시 멀쩡한 이미지 삭제 방지
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                oldKeys.forEach(s3ImageUploader::delete);
            }
        });
    }

    // 목록 화면에서 게시글에 이미지가 포함되어 있는지 여부 확인
    public boolean hasImage(String content) {
        if (content == null) {
            return false;
        }
        return Jsoup.parse(content).select("img").stream()
                .anyMatch(img -> StringUtils.hasText(img.attr("src")));
    }

    // ========== private 메서드 ==========

    // content의 <img> 중 해당 게시글(postId) 폴더 소속 key만 추출
    private Set<String> extractPostImageKeys(String content, Long postId) {
        String postPrefix = POSTS_PREFIX + "/" + postId + "/";

        return Jsoup.parse(content).select("img").stream()
                .map(img -> img.attr("src"))
                .map(s3ImageUploader::extractKeyIfOwned)
                .filter(Objects::nonNull)
                .filter(key -> key.startsWith(postPrefix)) // 다른 게시글 이미지는 절대 안 건드림
                .collect(Collectors.toSet());
    }

    // 게시글 저장 실패 시 S3에 복사된 이미지 삭제 (보상 처리)
    private void compensate(List<String> copiedKeys) {
        for (String key : copiedKeys) {
            s3ImageUploader.delete(key);
        }
    }

    private void registerMoveCleanup(List<String> copiedKeys, List<String> tempKeys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    // 커밋 성공 - 새 복사본이 확정됐으니 temp 원본은 더 이상 불필요
                    tempKeys.forEach(key -> {
                        try {
                            s3ImageUploader.delete(key);
                        } catch (Exception e) {
                            log.warn("temp 파일 삭제 실패 (Lifecycle이 처리) - key: {}", key);
                        }
                    });
                } else {
                    // 롤백 - 새 복사본이 고아가 됨, 즉시 정리. temp 원본은 보존해서 재시도 가능하게
                    copiedKeys.forEach(s3ImageUploader::delete);
                }
            }
        });
    }
}
