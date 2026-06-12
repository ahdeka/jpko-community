package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.category.service.CategoryService;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.comment.repository.PostCommentCount;
import com.jpkocommunity.domain.like.entity.LikeType;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.notice.dto.response.NoticeSummaryResponse;
import com.jpkocommunity.domain.notice.service.NoticeService;
import com.jpkocommunity.domain.post.dto.request.PostCreateRequest;
import com.jpkocommunity.domain.post.dto.request.PostUpdateRequest;
import com.jpkocommunity.domain.post.dto.response.*;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostImageRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;

    public PostListResponse getPostsByCategory(Long categoryId, Pageable pageable) {
        List<NoticeSummaryResponse> pinnedNotices = pageable.getPageNumber() == 0
                ? noticeService.getPinnedNotices()
                : List.of();

        Page<PostSummaryResponse> posts = toSummaryPage(
                postRepository.findByCategoryId(categoryId, pageable)
        );
        return PostListResponse.of(pinnedNotices, posts);
    }

    public PostListResponse getAllPosts(Pageable pageable) {
        List<NoticeSummaryResponse> pinnedNotices = pageable.getPageNumber() == 0
                ? noticeService.getPinnedNotices()
                : List.of();

        Page<PostSummaryResponse> posts = toSummaryPage(
                postRepository.findAllActive(pageable)
        );
        return PostListResponse.of(pinnedNotices, posts);
    }

    // 게시글 목록에 댓글 수를 함께 채워서 반환 (post당 쿼리 1번이 아닌, 페이지 전체 댓글 수를 한 번에 조회)
    private Page<PostSummaryResponse> toSummaryPage(Page<Post> posts) {
        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();

        Map<Long, Long> commentCounts = postIds.isEmpty()
                ? Map.of()
                : commentRepository.countByPostIdIn(postIds).stream()
                  .collect(Collectors.toMap(PostCommentCount::getPostId, PostCommentCount::getCommentCount));

        return posts.map(post -> PostSummaryResponse.from(post, commentCounts.getOrDefault(post.getId(), 0L)));
    }

    @Transactional
    public PostDetailResponse getPost(Long postId, Long currentUserId) {
        Post post = findActivePostById(postId);
        post.increaseViewCount();

        long likeCount = likeRepository.countByPostIdAndType(postId, LikeType.LIKE);
        long dislikeCount = likeRepository.countByPostIdAndType(postId, LikeType.DISLIKE);

        List<PostImageResponse> images = postImageRepository
                .findByPostIdOrderByDisplayOrderAsc(postId)
                .stream()
                .map(PostImageResponse::from)
                .toList();

        return PostDetailResponse.from(post, likeCount, dislikeCount, currentUserId, images);
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