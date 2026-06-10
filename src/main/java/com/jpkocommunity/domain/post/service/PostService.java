package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.category.service.CategoryService;
import com.jpkocommunity.domain.like.entity.LikeType;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.post.dto.request.PostCreateRequest;
import com.jpkocommunity.domain.post.dto.request.PostUpdateRequest;
import com.jpkocommunity.domain.post.dto.response.PostDetailResponse;
import com.jpkocommunity.domain.post.dto.response.PostResponse;
import com.jpkocommunity.domain.post.dto.response.PostSummaryResponse;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    public Page<PostSummaryResponse> getPostsByCategory(Long categoryId, Pageable pageable) {
        return postRepository.findByCategoryId(categoryId, pageable)
                .map(PostSummaryResponse::from);
    }

    public Page<PostSummaryResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAllActive(pageable)
                .map(PostSummaryResponse::from);
    }

    @Transactional
    public PostDetailResponse getPost(Long postId) {
        Post post = findActivePostById(postId);
        post.increaseViewCount();

        long likeCount    = likeRepository.countByPostIdAndType(postId, LikeType.LIKE);
        long dislikeCount = likeRepository.countByPostIdAndType(postId, LikeType.DISLIKE);

        return PostDetailResponse.from(post, likeCount, dislikeCount);
    }

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request, String ipAddress) {
        User user = userService.findById(userId);
        Category category = categoryService.findById(request.categoryId());

        Post post = Post.builder()
                .user(user)
                .category(category)
                .title(request.title())
                .content(request.content())
                .anonymous(request.anonymous())
                .ipAddress(ipAddress)
                .build();

        postRepository.save(post);

        return PostResponse.from(post.getId());
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request) {
        Post post = findActivePostById(postId);

        post.update(request.title(), request.content());

        return PostResponse.from(post.getId());
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = findActivePostById(postId);
        validateAuthor(post, userId);
        post.delete();
    }

    public Post findActivePostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    // ========== private 메서드 ==========

    void validateAuthor(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}