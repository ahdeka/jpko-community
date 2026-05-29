package com.jpkocommunity.domain.like.service;

import com.jpkocommunity.domain.like.dto.request.LikeRequest;
import com.jpkocommunity.domain.like.dto.response.LikeResponse;
import com.jpkocommunity.domain.like.entity.Like;
import com.jpkocommunity.domain.like.entity.LikeType;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.service.PostService;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostService postService;
    private final UserService userService;

    @Transactional
    public LikeResponse toggle(Long userId, Long postId, LikeRequest request) {
        Post post = postService.findActivePostById(postId);
        User user = userService.findById(userId);

        Optional<Like> existing = likeRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isEmpty()) {
            // 케이스 1: 처음 누름 → 새로 저장
            try {
                likeRepository.saveAndFlush(Like.builder()
                        .post(post)
                        .user(user)
                        .type(request.type())
                        .build());
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 unique 제약 위반 발생 시
                throw new CustomException(ErrorCode.DUPLICATE_LIKE);
            }
        } else {
            Like like = existing.get();

            if (like.getType() == request.type()) {
                // 케이스 2: 같은 타입 재클릭 → 취소
                likeRepository.delete(like);
            } else {
                // 케이스 3: 다른 타입 클릭 → 변경
                like.changeType(request.type());
            }
        }

        return buildResponse(postId, userId);
    }

    public LikeResponse getLikeStatus(Long postId, Long userId) {
        postService.findActivePostById(postId);
        return buildResponse(postId, userId);
    }

    // ========== private 메서드 ==========

    private LikeResponse buildResponse(Long postId, Long userId) {
        long likeCount    = likeRepository.countByPostIdAndType(postId, LikeType.LIKE);
        long dislikeCount = likeRepository.countByPostIdAndType(postId, LikeType.DISLIKE);

        LikeType myType = null;
        if (userId != null) {
            myType = likeRepository.findByPostIdAndUserId(postId, userId)
                    .map(Like::getType)
                    .orElse(null);
        }

        return new LikeResponse(likeCount, dislikeCount, myType);
    }
}