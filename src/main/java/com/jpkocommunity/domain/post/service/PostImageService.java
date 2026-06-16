package com.jpkocommunity.domain.post.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private static final int MAX_IMAGES_PER_POST = 5;

    private final S3ImageUploader s3ImageUploader;
    private final PostImageRepository postImageRepository;

    /**
     * 게시글 생성 시 이미지 여러장 업로드
     * PostService.createPost()에서 @Transactional 안에서 호출함
     */
    public void uploadAll(Post post, List<MultipartFile> files) {
        if (files.size() > MAX_IMAGES_PER_POST) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        List<String> uploadedKeys = new ArrayList<>();

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                // 빈 파일은 건너뛰기
                if (file == null || file.isEmpty()) continue;

                String prefix = "posts/" + post.getId();
                S3UploadResult result = s3ImageUploader.upload(file, prefix);
                uploadedKeys.add(result.s3Key());  // 롤백 추적용

                postImageRepository.save(PostImage.builder()
                        .post(post)
                        .s3Key(result.s3Key())
                        .imageUrl(result.imageUrl())
                        .displayOrder(i)
                        .build());
            }
        } catch (Exception e) {
            // S3 업로드는 성공했지만 DB 저장에서 예외 발생 시, 업로드된 S3 객체들 삭제 (보상 트랜잭션)
            log.error("이미지 업로드 중 오류 발생, S3 보상 삭제 시작 - {}개", uploadedKeys.size());
            uploadedKeys.forEach(key -> {
                try {
                    s3ImageUploader.delete(key);
                } catch (Exception deleteEx) {
                    log.error("S3 보상 삭제 실패 (수동 정리 필요) - key: {}", key);
                }
            });
            throw e;  // 원래 예외를 다시 던져서 트랜잭션 롤백 유도
        }
    }

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
