package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.category.entity.Category;
import com.jpkocommunity.domain.category.service.CategoryService;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.comment.repository.PostCommentCount;
import com.jpkocommunity.domain.image.service.ImageService;
import com.jpkocommunity.domain.like.entity.LikeType;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.like.repository.PostLikeCount;
import com.jpkocommunity.domain.notice.dto.response.NoticeSummaryResponse;
import com.jpkocommunity.domain.notice.service.NoticeService;
import com.jpkocommunity.domain.post.dto.request.PostCreateRequest;
import com.jpkocommunity.domain.post.dto.request.PostUpdateRequest;
import com.jpkocommunity.domain.post.dto.request.SearchType;
import com.jpkocommunity.domain.post.dto.response.PostDetailResponse;
import com.jpkocommunity.domain.post.dto.response.PostListResponse;
import com.jpkocommunity.domain.post.dto.response.PostResponse;
import com.jpkocommunity.domain.post.dto.response.PostSummaryResponse;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_POPULAR_LIMIT = 50;
    private static final int MAX_POPULAR_DAYS = 30;
    private static final int EDIT_ALLOWED_MINUTES = 30;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;
    private final ImageService imageService;

    private List<PostSummaryResponse> toSummaryList(List<Post> posts) {
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        if (postIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> commentCounts = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostCommentCount::getPostId, PostCommentCount::getCommentCount));

        Map<Long, Long> likeCounts = likeRepository.countLikesByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostLikeCount::getPostId, PostLikeCount::getLikeCount));

        return posts.stream()
                .map(post -> PostSummaryResponse.from(
                        post,
                        commentCounts.getOrDefault(post.getId(), 0L),
                        likeCounts.getOrDefault(post.getId(), 0L),
                        imageService.hasImage(post.getContent())
                ))
                .toList();
    }

    private Page<PostSummaryResponse> toSummaryPage(Page<Post> posts) {
        List<PostSummaryResponse> content = toSummaryList(posts.getContent());
        return new PageImpl<>(content, posts.getPageable(), posts.getTotalElements());
    }

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

    public PostListResponse searchPosts(String keyword, SearchType type, Long categoryId, Pageable pageable) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.length() < MIN_KEYWORD_LENGTH) {
            // 전용 에러 코드로 프론트가 정확히 분기할 수 있게
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }

        Page<Post> result = (type == SearchType.TITLE)
                ? postRepository.searchByTitle(categoryId, trimmed, pageable)
                : postRepository.searchByTitleAndContent(categoryId, trimmed, pageable);

        return PostListResponse.of(List.of(), toSummaryPage(result));
    }


    /**
     * 인기 게시글 조회
     * @param days  최근 N일 (1=실시간, 7=주간)
     * @param limit 노출 개수
     */
    public List<PostSummaryResponse> getPopularPosts(int days, int limit) {
        // 잘못된 파라미터로 500 떨어지는 걸 막기 위해 clamp
        int safeDays = Math.clamp(days, 1, MAX_POPULAR_DAYS);
        int safeLimit = Math.clamp(limit, 1, MAX_POPULAR_LIMIT);

        LocalDateTime since = LocalDateTime.now().minusDays(safeDays);
        List<Post> posts = postRepository.findPopularPosts(since, PageRequest.of(0, safeLimit));
        return toSummaryList(posts);
    }

    @Transactional
    public PostDetailResponse getPost(Long postId, Long currentUserId) {
        Post post = findActivePostById(postId);
        post.increaseViewCount();

        long likeCount = likeRepository.countByPostIdAndType(postId, LikeType.LIKE);
        long dislikeCount = likeRepository.countByPostIdAndType(postId, LikeType.DISLIKE);

        return PostDetailResponse.from(post, likeCount, dislikeCount, currentUserId);
    }

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request, String ipAddress) {
        User user = userService.findById(userId);
        Category category = categoryService.findById(request.categoryId());

        String sanitizedContent = validateContent(request.content());

        // 게시글 저장 -> postId 생성
        Post post = postRepository.save(Post.builder()
                .user(user)
                .category(category)
                .title(request.title())
                .content(sanitizedContent)
                .anonymous(request.anonymous())
                .ipAddress(ipAddress)
                .build());

        String movedContent = imageService.moveTempImagesToPost(sanitizedContent, post.getId());
        post.confirmContent(movedContent);
        return PostResponse.from(post.getId());
    }

    @Transactional
    public PostResponse updatePost(AuthUser authUser, Long postId, PostUpdateRequest request) {
        Post post = findActivePostById(postId);
        validateAuthor(post, authUser);

        // 시간 제한 체크, ADMIN은 우회
        boolean isAdmin = authUser.role() == UserRole.ADMIN;
        if (!isAdmin && post.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(EDIT_ALLOWED_MINUTES))) {
            throw new CustomException(ErrorCode.POST_EDIT_EXPIRED);
        }

        Category category = categoryService.findById(request.categoryId());

        String oldContent = post.getContent(); // 삭제된 이미지 비교용
        String sanitizedContent = validateContent(request.content());
        String movedContent = imageService.moveTempImagesToPost(sanitizedContent, postId);
        imageService.deleteOrphanedImages(oldContent, movedContent, postId);

        post.update(category, request.title(), movedContent);

        return PostResponse.from(post.getId());
    }

    @Transactional
    public void deletePost(AuthUser authUser, Long postId) {
        Post post = findActivePostById(postId);
        validateAuthor(post, authUser);
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

    void validateAuthor(Post post, AuthUser authUser) {
        boolean isOwner = post.getUser().getId().equals(authUser.userId());
        boolean isAdmin = authUser.role() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private String validateContent(String content) {
        String sanitizedContent = imageService.sanitize(content);
        imageService.validateImageCount(sanitizedContent);
        return sanitizedContent;
    }

}