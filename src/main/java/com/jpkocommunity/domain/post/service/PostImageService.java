package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.post.entity.PostImage;
import com.jpkocommunity.domain.post.repository.PostImageRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.infra.s3.S3ImageUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private static final int MAX_IMAGES_PER_POST = 5;

    private final S3ImageUploader s3ImageUploader;
    private final PostImageRepository postImageRepository;

    /**
     * 게시글 이미지 삭제
     * PostController.deleteImage() 엔드포인트에서 사용
     */
    @Transactional
    public void delete(Long userId, Long postId, Long imageId) {
        PostImage image = postImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        if (!image.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        s3ImageUploader.delete(image.getS3Key());
        postImageRepository.delete(image);
    }

}
