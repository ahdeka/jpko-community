package com.jpkocommunity.global.infra.s3;

import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageUploader {

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("jpg", "jpeg", "png", "gif", "webp");

    private static final Map<String, String> EXTENSION_CONTENT_TYPE = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "gif",  "image/gif",
            "webp", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public S3UploadResult upload(MultipartFile file, String s3KeyPrefix) {
        String extension = extractExtension(file.getOriginalFilename());
        validate(file, extension);

        String s3Key = buildKey(s3KeyPrefix, extension);
        putObject(file, s3Key, extension);

        return new S3UploadResult(s3Key, buildUrl(s3Key));
    }

    public String copy(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyRequest);

            return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + destinationKey;

        } catch (SdkException e) {
            log.error("S3 copy 실패 - source: {}, dest: {}", sourceKey, destinationKey);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
        } catch (SdkException e) {
            log.error("S3 삭제 실패 (수동 정리 필요) - key: {}, error: {}", s3Key, e.getMessage());
        }
    }

    // ========== private 메서드============

    private void validate(MultipartFile file, String extension) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void putObject(MultipartFile file, String s3Key, String extension) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(EXTENSION_CONTENT_TYPE.getOrDefault(extension, "application/octet-stream"))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        } catch (SdkException e) {
            log.error("S3 업로드 실패 - key: {}, error: {}", s3Key, e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (IOException e) {
            log.error("파일 읽기 실패 - key: {}", s3Key);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** prefix + UUID + 확장자 조합으로 유일한 키 생성 */
    private String buildKey(String prefix, String extension) {
        return String.format("%s/%s.%s", prefix, UUID.randomUUID(), extension);
    }

    private String buildUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);
    }

}
