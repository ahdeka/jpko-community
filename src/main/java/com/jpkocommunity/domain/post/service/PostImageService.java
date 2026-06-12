package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.post.dto.response.PostImageResponse;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.entity.PostImage;
import com.jpkocommunity.domain.post.repository.PostImageRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private static final int MAX_IMAGES_PER_POST = 5;

    private final S3ImageUploader s3ImageUploader;
    private final PostService postService;
    private final PostImageRepository postImageRepository;

    @Transactional
    public PostImageResponse upload(Long userId, Long postId, MultipartFile file, int displayOrder) {
        Post post = postService.findActivePostById(postId);
        postService.validateAuthor(post, userId);

        if (postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId).size() >= MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        String prefix = "posts/" + postId;
        S3UploadResult result = s3ImageUploader.upload(file, prefix);

        try {
            PostImage saved = postImageRepository.save(PostImage.builder()
                    .post(post)
                    .s3Key(result.s3Key())
                    .imageUrl(result.imageUrl())
                    .displayOrder(displayOrder)
                    .build());
            return PostImageResponse.from(saved);
        } catch (Exception e) {
            log.error("DB 저장 실패, S3 보상 삭제 - key: {}", result.s3Key(), e);
            s3ImageUploader.delete(result.s3Key());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
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

        // DB 커밋 성공 후에만 S3 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3ImageUploader.delete(s3Key);
            }
        });
    }


}
