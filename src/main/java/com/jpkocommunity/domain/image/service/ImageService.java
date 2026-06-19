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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // 게시글 저장 시 임시 이미지들을 posts/{postId}로 이동
    public String moveTempImagesToPost(String content, Long postId) {
        Matcher matcher = TEMP_URL_PATTERN.matcher(content);
        List<String[]> replacements = new ArrayList<>();
        List<String> copiedKeys = new ArrayList<>(); // 성공한 S3 키 추적

        while (matcher.find()) {
            String originalUrl = matcher.group(0);           // 전체 URL
            String tempKey = matcher.group(1);               // "temp/uuid.jpg"
            String filename = tempKey.substring(TEMP_PREFIX.length()); // "uuid.jpg"
            String newKey = POSTS_PREFIX + postId + "/" + filename;    // "posts/1/uuid.jpg"

            try {
                String newUrl = s3ImageUploader.copy(tempKey, newKey);
                replacements.add(new String[]{originalUrl, newUrl, tempKey});
                copiedKeys.add(newKey); // copy 성공한 시점만 기록
            } catch (Exception e) {
                log.error("S3 copy 실패 - tempKey: {}, postId: {}", tempKey, postId);
                compensate(copiedKeys);
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        // 여기 도달 = 모든 copy 성공
        String updatedContent = content;
        for (String[] r : replacements) {
            updatedContent = updatedContent.replace(r[0], r[1]); // URL 치환
            try {
                s3ImageUploader.delete(r[2]); // temp 원본 삭제
            } catch (Exception e) {
                log.warn("temp 파일 삭제 실패 (Lifecycle이 처리) - key: {}", r[2]);
            }
        }

        return updatedContent;
    }

    // HTML content 저장 전 악성 태그 제거 -> 안전한 HTML만 DB에 저장 *XSS 방어
    public String sanitize(String html) {
        return Jsoup.clean(html, Safelist.relaxed());
    }

    // 게시글당 이미지 수 제한 검증 -> PostService.createPost() 에서 사용
    public void validateImageCount(String content) {
        int count = Jsoup.parse(content).select("img").size();
        if (count > MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
    }

    // 게시글 저장 실패 시 S3에 복사된 이미지 삭제 (보상 처리)
    private void compensate(List<String> copiedKeys) {
        for (String key : copiedKeys) {
            s3ImageUploader.delete(key);
        }
    }
}
