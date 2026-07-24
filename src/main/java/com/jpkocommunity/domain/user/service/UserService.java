package com.jpkocommunity.domain.user.service;

import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.comment.repository.PostCommentCount;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.like.repository.PostLikeCount;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.dto.request.UpdateBioRequest;
import com.jpkocommunity.domain.user.dto.request.UpdateNicknameRequest;
import com.jpkocommunity.domain.user.dto.request.UpdatePasswordRequest;
import com.jpkocommunity.domain.user.dto.request.WithdrawRequest;
import com.jpkocommunity.domain.user.dto.response.*;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserGrade;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.event.UserWithdrawnEvent;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LikeRepository likeRepository;

    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }


    // =========== 공개 프로필 기능 ==========

    private User findPublicUserByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 탈퇴된 유저는 조회 X (정지는 OK)
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    public PublicProfileResponse getUserProfile(String nickname) {
        return PublicProfileResponse.from(findPublicUserByNickname(nickname));
    }

    public Page<UserPostResponse> getUserPosts(String nickname, Pageable pageable) {
        User user = findPublicUserByNickname(nickname);
        Page<Post> posts = postRepository.findPublicPostsByUserId(user.getId(), pageable);

        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();

        if (postIds.isEmpty()) {
            return posts.map(post -> UserPostResponse.from(post, 0L, 0L));
        }

        Map<Long, Long> commentCounts = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostCommentCount::getPostId, PostCommentCount::getCommentCount));

        Map<Long, Long> likeCounts = likeRepository.countLikesByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostLikeCount::getPostId, PostLikeCount::getLikeCount));

        return posts.map(post -> UserPostResponse.from(
                post,
                commentCounts.getOrDefault(post.getId(), 0L),
                likeCounts.getOrDefault(post.getId(), 0L)
        ));
    }

    // =========== 마이페이지 기능 ==========

    public Page<MyPostResponse> getMyPosts(Long userId, Pageable pageable) {
        return postRepository.findByUserIdWithCategory(userId, pageable)
                .map(MyPostResponse::from);
    }

    public Page<MyCommentResponse> getMyComments(Long userId, Pageable pageable) {
        return commentRepository.findByUserIdWithPost(userId, pageable)
                .map(MyCommentResponse::from);
    }

    @Transactional
    public void updateNickname(Long userId, UpdateNicknameRequest request) {
        User user = findById(userId);

        if (user.getNickname().equals(request.nickname())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_NICKNAME);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(request.nickname());
    }

    @Transactional
    public void updateBio(Long userId, UpdateBioRequest request) {
        User user = findById(userId);
        user.updateBio(normalizeBio(request.bio()));
    }

    private String normalizeBio(String bio) {
        if (bio == null) return null;
        String trimmed = bio.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = findById(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));

        // 사용자 비밀번호 변경 시, 기존 refresh token 삭제
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findById(userId);

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        user.withdraw();

        // 모든 기기 로그아웃 처리
        refreshTokenRepository.deleteByUserId(userId);

        // 사용자 탈퇴 이벤트는 탈퇴 트랜잭션 커밋 후 처리됨
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
    }

    // ========== 관리자 기능 ==========

    public Page<AdminUserResponse> getUsers(String keyword, Pageable pageable) {
        Page<User> users = (keyword == null || keyword.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByNicknameContainingOrEmailContaining(keyword, keyword, pageable);
        return users.map(AdminUserResponse::from);
    }

    @Transactional
    public void updateStatus(Long userId, UserStatus status) {
        if (status == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        User user = findById(userId);

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }

        if (status == UserStatus.SUSPENDED) {
            if (user.getRole() == UserRole.ADMIN) {
                throw new CustomException(ErrorCode.CANNOT_SUSPEND_ADMIN);
            }
            user.suspend();
            refreshTokenRepository.deleteByUserId(userId);
        } else {
            user.activate();
        }
    }

    @Transactional
    public void updateGrade(Long userId, UserGrade grade) {
        User user = findById(userId);

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }

        if (grade == UserGrade.SHOGUN && user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.SHOGUN_REQUIRES_ADMIN_ROLE);
        }

        user.updateGrade(grade);
    }

}