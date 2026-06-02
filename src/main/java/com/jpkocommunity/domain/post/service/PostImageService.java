package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.post.dto.response.PostImageResponse;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.entity.PostImage;
import com.jpkocommunity.domain.post.repository.PostImageRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private static final int MAX_IMAGES_PER_POST = 5;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Map<String, String> EXTENSION_CONTENT_TYPE = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "gif",  "image/gif",
            "webp", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final S3Client s3Client;
    private final PostService postService;
    private final PostImageRepository postImageRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Transactional
    public PostImageResponse upload(Long userId, Long postId, MultipartFile file, int displayOrder) {
        Post post = postService.findActivePostById(postId);
        postService.validateAuthor(post, userId);

        if (postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId).size() >= MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        String extension = getExtension(file.getOriginalFilename());
        validate(file, extension);

        String s3Key = generateKey(postId, extension);
        uploadToS3(file, s3Key, extension);

        try {
            PostImage saved = postImageRepository.save(PostImage.builder()
                    .post(post)
                    .s3Key(s3Key)
                    .imageUrl(buildImageUrl(s3Key))
                    .displayOrder(displayOrder)
                    .build());
            return PostImageResponse.from(saved);
        } catch (Exception e) {
            log.error("DB 저장 실패, S3 보상 삭제 - key: {}", s3Key, e);
            deleteFromS3(s3Key);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public List<PostImageResponse> getImages(Long postId) {
        return postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId)
                .stream()
                .map(PostImageResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long postId, Long imageId) {
        Post post = postService.findActivePostById(postId);
        postService.validateAuthor(post, userId);

        PostImage postImage = postImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        if (!postImage.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String s3Key = postImage.getS3Key();
        postImageRepository.delete(postImage);

        // DB 커밋 성공 후에만 S3 삭제 — 커밋 실패 시 S3 파일이 보존됨
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFromS3(s3Key);
            }
        });
    }

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

    private void uploadToS3(MultipartFile file, String s3Key, String extension) {
        try {
            String contentType = EXTENSION_CONTENT_TYPE.getOrDefault(extension, "application/octet-stream");

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
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

    private void deleteFromS3(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
        } catch (SdkException e) {
            log.error("S3 삭제 실패 - key: {}, error: {}", s3Key, e.getMessage());
        }
    }

    private String generateKey(Long postId, String extension) {
        return String.format("posts/%d/%s.%s", postId, UUID.randomUUID(), extension);
    }

    private String buildImageUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
