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
import com.jpkocommunity.domain.post.entity.PostTag;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.post.repository.PostTagRepository;
import com.jpkocommunity.domain.tag.service.TagService;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;

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
        saveTags(post, request.tags());

        return PostResponse.from(post.getId());
    }

    @Transactional
    public PostResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
        Post post = findActivePostById(postId);
        validateAuthor(post, userId);

        post.update(request.title(), request.content());

        post.getPostTags().clear();
        postTagRepository.flush();
        saveTags(post, request.tags());

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

    private void validateAuthor(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    // 태그 저장 공통 메서드
    // null이나 빈 리스트면 태그 없이 저장
    private void saveTags(Post post, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;

        tagNames.stream()
                .map(tagService::findOrCreate)  // 없으면 생성, 있으면 조회
                .map(tag -> PostTag.builder().post(post).tag(tag).build())
                .forEach(postTag -> post.getPostTags().add(postTag));
    }
}