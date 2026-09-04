package com.jpkocommunity.domain.user.service;

import com.jpkocommunity.domain.auth.dto.response.UserInfoResponse;
import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.comment.repository.PostCommentCount;
import com.jpkocommunity.domain.level.entity.UserLevel;
import com.jpkocommunity.domain.level.repository.UserLevelRepository;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.like.repository.PostLikeCount;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.event.ContentDeletedEvent;
import com.jpkocommunity.domain.user.dto.request.UpdateBioRequest;
import com.jpkocommunity.domain.user.dto.request.UpdateNicknameRequest;
import com.jpkocommunity.domain.user.dto.request.UpdatePasswordRequest;
import com.jpkocommunity.domain.user.dto.request.WithdrawRequest;
import com.jpkocommunity.domain.user.dto.response.*;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.event.UserWithdrawnEvent;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // ROOT만 무기한 정지 가능, ADMIN은 항상 이 기간 이하로만 정지할 수 있다.
    private static final long MAX_ADMIN_SUSPEND_DAYS = 7;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LikeRepository likeRepository;
    private final UserLevelRepository userLevelRepository;

    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public List<User> findAllByIdIn(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    // 일일 포인트 룰렛 피해자 선정용
    public Optional<Long> findRandomActiveUserIdExcluding(Long excludeUserId) {
        return userRepository.findRandomActiveUserIdExcluding(excludeUserId);
    }

    public UserInfoResponse getMyInfo(Long userId) {
        User user = findById(userId);
        return UserInfoResponse.from(user, expOf(userId));
    }

    // =========== 공개 프로필 기능 ==========

    private User findPublicUserByNickname(String nickname) {
        User user = userRepository.findByNicknameIgnoreCase(nickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateWithdrawUser(user);

        return user;
    }

    public UserProfileResponse getUserProfile(String nickname) {
        User user = findPublicUserByNickname(nickname);
        return UserProfileResponse.from(user, expOf(user.getId()));
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
        return commentRepository.findMyPostComments(userId, pageable);
    }

    @Transactional
    public void updateNickname(Long userId, UpdateNicknameRequest request) {
        User user = findById(userId);
        String newNickname = request.nickname();

        if (user.getNickname().equals(newNickname)) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_NICKNAME);
        }

        // TODO 닉네임 변경 쿨타임 주기 메서드 작성하기

        if (userRepository.existsByNicknameIgnoreCaseAndIdNot(newNickname, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(request.nickname());

        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
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
        validateWithdrawUser(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        user.withdraw();

        // 모든 기기 로그아웃 처리
        refreshTokenRepository.deleteByUserId(userId);

        // 사용자 탈퇴 이벤트는 탈퇴 트랜잭션 커밋 후 처리됨
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));

        // 해당 유저 PENDING 상태 신고 모두 RESOLVED 처리
        eventPublisher.publishEvent(new ContentDeletedEvent(ReportTargetType.USER, userId));
    }

    // ========== 관리자 기능 ==========

    public Page<AdminUserResponse> getUsers(String keyword, Pageable pageable) {
        Page<User> users = (keyword == null || keyword.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);

        return users.map(AdminUserResponse::from);
    }

    // 유저 정지는 ADMIN/ROOT만 할 수 있는 관리 기능이다. 포인트로 사고 파는 경로는 없고,
    // User.status(+suspendedUntil) 한 곳에서만 상태를 관리한다.
    @Transactional
    public void updateStatus(Long userId, UserStatus status, LocalDateTime suspendedUntil, Long actingUserId) {
        if (status == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        User user = findById(userId);
        validateWithdrawUser(user);
        User actingUser = findById(actingUserId);

        if (status == UserStatus.SUSPENDED) {
            if (user.getRole() == UserRole.ROOT) {
                throw new CustomException(ErrorCode.CANNOT_SUSPEND_ROOT);
            }

            // ROOT만 무기한 정지 가능. ADMIN은 항상 기한이 있는 정지만 가능하다
            // — 진짜 관리자는 ROOT뿐이라는 원칙.
            if (actingUser.getRole() == UserRole.ADMIN) {
                if (suspendedUntil == null) {
                    throw new CustomException(ErrorCode.SUSPEND_DURATION_REQUIRED);
                }
                if (suspendedUntil.isAfter(LocalDateTime.now().plusDays(MAX_ADMIN_SUSPEND_DAYS))) {
                    throw new CustomException(ErrorCode.SUSPEND_DURATION_TOO_LONG);
                }
            }

            user.suspend(suspendedUntil);
            refreshTokenRepository.deleteByUserId(userId);
        } else {
            user.activate();
        }
    }

    // 정지된 본인이 사유·해제 시각을 확인하는 용도 (SuspendAllowedPaths로 정지 중에도 호출 가능).
    public SuspensionStatusResponse getMySuspensionStatus(Long userId) {
        User user = findById(userId);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            return SuspensionStatusResponse.notSuspended();
        }
        return SuspensionStatusResponse.suspended(user.getSuspendedUntil());
    }

    // ========== 최고 관리자 기능 ==========

    @Transactional
    public void updateRole(Long userId, UserRole role) {
        User user = findById(userId);
        validateWithdrawUser(user);

        // 최고 관리자 계정은 변경 불가
        if (user.getRole() == UserRole.ROOT) {
            throw new CustomException(ErrorCode.CANNOT_MODIFY_ROOT);
        }

        // ROOT는 부여 불가
        if (role == UserRole.ROOT) {
            throw new CustomException(ErrorCode.CANNOT_ASSIGN_ROOT_ROLE);
        }

        user.updateRole(role);
    }

    // ROOT가 ADMIN 권한을 즉시 박탈한다. JwtAuthenticationFilter가 매 요청마다 role을 DB에서
    // 다시 확인하므로, 이 변경은 대상의 다음 요청부터 곧바로(로그인 세션 종료 없이) 반영된다.
    @Transactional
    public void revokeAdmin(Long userId) {
        User user = findById(userId);

        if (user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.TARGET_NOT_ADMIN);
        }

        user.updateRole(UserRole.USER);
    }

    // ========== private 메서드 ==========

    private void validateWithdrawUser(User user) {
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }
    }

    private long expOf(Long userId) {
        return userLevelRepository.findByUserId(userId)
                .map(UserLevel::getExp)
                .orElse(0L);
    }

}
