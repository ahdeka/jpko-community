package com.jpkocommunity.domain.image.service;

import com.jpkocommunity.domain.image.dto.ImageUploadResponse;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.infra.s3.S3ImageUploader;
import com.jpkocommunity.global.infra.s3.S3UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3ImageUploader s3ImageUploader;

    private static final int MAX_IMAGES_PER_CONTENT = 10;
    private static final int MAX_VIDEOS_PER_CONTENT = 2;
    private static final String TEMP_PREFIX = "temp";
    private static final String POSTS_PREFIX = "posts";
    private static final String NOTICES_PREFIX = "notices";
    private static final String COMMENTS_PREFIX = "comments";

    // content HTML에서 임시 이미지 URL 추출을 위한 패턴
    private static final Pattern TEMP_URL_PATTERN =
            Pattern.compile("https?://[^/]+/(temp/[^\"\\s>]+)");

    // ========== Post 용 ==========

    public String moveTempMediaToPost(String content, Long postId) {
        return moveTempMedia(content, POSTS_PREFIX + "/" + postId);
    }

    public void deleteOrphanedMedia(String oldContent, String newContent, Long postId) {
        deleteOrphanedMediaByFolder(oldContent, newContent, POSTS_PREFIX + "/" + postId);
    }

    // ========== Notice 용 ==========

    public String moveTempMediaToNotice(String content, Long noticeId) {
        return moveTempMedia(content, NOTICES_PREFIX + "/" + noticeId);
    }

    public void deleteOrphanedMediaForNotice(String oldContent, String newContent, Long noticeId) {
        deleteOrphanedMediaByFolder(oldContent, newContent, NOTICES_PREFIX + "/" + noticeId);
    }

    public void deleteAllNoticeMedia(String content, Long noticeId) {
        Set<String> keys = extractMediaKeys(content, NOTICES_PREFIX + "/" + noticeId);
        if (keys.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(s3ImageUploader::delete);
            }
        });
    }

    // ========== Comment 용 ==========

    // temp 이미지를 comments/ 로 이동 -> 커밋 시 temp 이미지 삭제
    public String moveTempMediaToComment(String tempUrl) {
        String tempKey = s3ImageUploader.extractKeyIfOwned(tempUrl);
        if (tempKey == null || !tempKey.startsWith(TEMP_PREFIX + "/")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String filename = tempKey.substring(TEMP_PREFIX.length() + 1);
        validateNotVideo(filename);

        String newKey = COMMENTS_PREFIX + "/" + filename;
        String newUrl = s3ImageUploader.copy(tempKey, newKey);

        registerMoveCleanup(List.of(newKey), List.of(tempKey));
        return newUrl;
    }

    // 댓글 하드 삭제에 따른 이미지 파일 정리
    public void deleteCommentImages(List<String> imageUrls) {
        List<String> keys = imageUrls.stream()
                .map(s3ImageUploader::extractKeyIfOwned) // 우리 버킷 소속만
                .filter(Objects::nonNull)
                .toList();
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
        validateMediaCount(sanitized);
        return sanitized;
    }

    // 목록에서 미디어 파일 포함 여부 확인
    public boolean hasImage(String content) {
        if (content == null) {
            return false;
        }
        Document doc = Jsoup.parse(content);
        return doc.select("img").stream().anyMatch(el ->
                StringUtils.hasText(el.attr("src")))
                || doc.select("video[src], video source[src]").stream().anyMatch(el ->
                StringUtils.hasText(el.attr("src")));
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
        Safelist safelist = Safelist.relaxed()
                .addTags("hr", "video", "source") // hr, 동영상 태그 허용
                .addAttributes("video", "src", "controls", "width", "height", "poster", "preload", "playsinline")
                .addAttributes("source", "src", "type")
                .addProtocols("video", "src", "http", "https")
                .addProtocols("video", "poster", "http", "https")
                .addProtocols("source", "src", "http", "https");
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false); // 연속 공백 보존
        return Jsoup.clean(html, "", safelist, settings);
    }

    private void validateMediaCount(String content) {
        Document doc = Jsoup.parse(content);
        if (doc.select("img").size() > MAX_IMAGES_PER_CONTENT) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        if (doc.select("video").size() > MAX_VIDEOS_PER_CONTENT) {
            throw new CustomException(ErrorCode.VIDEO_LIMIT_EXCEEDED);
        }
    }

    private void validateNotVideo(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (ext.equals("mp4") || ext.equals("mov")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String moveTempMedia(String content, String targetFolder) {
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
    private void deleteOrphanedMediaByFolder(String oldContent, String newContent, String folder) {
        Set<String> oldKeys = extractMediaKeys(oldContent, folder);
        Set<String> newKeys = extractMediaKeys(newContent, folder);
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
    private Set<String> extractMediaKeys(String content, String folder) {
        String prefix = folder + "/";
        Document doc = Jsoup.parse(content);
        Stream<String> srcs = Stream.concat(
                doc.select("img").stream().map(el -> el.attr("src")),
                doc.select("video[src], video source[src]").stream().map(el -> el.attr("src"))
        );
        return srcs
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
