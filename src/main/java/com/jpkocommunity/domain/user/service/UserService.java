package com.jpkocommunity.domain.user.service;

import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.comment.repository.PostCommentCount;
import com.jpkocommunity.domain.like.repository.LikeRepository;
import com.jpkocommunity.domain.like.repository.PostLikeCount;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.event.ContentDeletedEvent;
import com.jpkocommunity.domain.settings.service.SuspendSettingsService;
import com.jpkocommunity.domain.shop.entity.ActivityBlock;
import com.jpkocommunity.domain.shop.entity.RolePurchase;
import com.jpkocommunity.domain.shop.entity.RolePurchaseStatus;
import com.jpkocommunity.domain.shop.repository.ActivityBlockRepository;
import com.jpkocommunity.domain.shop.repository.RolePurchaseRepository;
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
    private final RolePurchaseRepository rolePurchaseRepository;
    private final ActivityBlockRepository activityBlockRepository;
    private final SuspendSettingsService suspendSettingsService;

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


    // =========== 공개 프로필 기능 ==========

    private User findPublicUserByNickname(String nickname) {
        User user = userRepository.findByNicknameIgnoreCase(nickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateWithdrawUser(user);

        return user;
    }

    public UserProfileResponse getUserProfile(String nickname) {
        return UserProfileResponse.from(findPublicUserByNickname(nickname));
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

        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, LocalDateTime> activeBlockExpiryByUserId = activityBlockRepository
                .findActiveByTargetIds(userIds, LocalDateTime.now()).stream()
                .collect(Collectors.toMap(
                        ActivityBlockRepository.ActiveBlockProjection::getTargetId,
                        ActivityBlockRepository.ActiveBlockProjection::getExpiresAt
                ));

        return users.map(user -> AdminUserResponse.from(user, activeBlockExpiryByUserId.get(user.getId())));
    }

    // ADMIN(진짜 관리자가 아닌, 경매/수동임명 여부와 무관하게)의 정지는 User.status를 건드리지 않고
    // ActivityBlock으로 완전히 대체한다 - 그래야 이 정지도 BlockAppeal로 ROOT가 감시할 수 있고
    // (기존 SuspendAllowedPaths엔 이의제기 경로가 없었다), 대상도 상점 차단과 동일하게
    // 자가해제(unblockSelf, 포인트는 SuspendUnlockSetting 가격 재사용)할 수 있다. ROOT의 정지만
    // 예전처럼 진짜 User.status=SUSPENDED로 남는다 — "진짜 관리자는 ROOT뿐"이라는 원칙.
    @Transactional
    public void updateStatus(Long userId, UserStatus status, LocalDateTime suspendedUntil, Long actingUserId) {
        if (status == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        User user = findById(userId);
        validateWithdrawUser(user);
        User actingUser = findById(actingUserId);
        boolean actingIsAdmin = actingUser.getRole() == UserRole.ADMIN;

        if (status == UserStatus.SUSPENDED) {
            if (user.getRole() == UserRole.ROOT) {
                throw new CustomException(ErrorCode.CANNOT_SUSPEND_ROOT);
            }

            // ROOT만 무기한 정지 가능. ADMIN(경매로 획득했든 ROOT가 수동으로 부여했든 상관없이)은
            // 항상 기한이 있는 정지만 가능하다 — 진짜 관리자는 ROOT뿐이라는 원칙.
            if (actingIsAdmin) {
                if (suspendedUntil == null) {
                    throw new CustomException(ErrorCode.SUSPEND_DURATION_REQUIRED);
                }
                if (suspendedUntil.isAfter(LocalDateTime.now().plusDays(MAX_ADMIN_SUSPEND_DAYS))) {
                    throw new CustomException(ErrorCode.SUSPEND_DURATION_TOO_LONG);
                }

                Long price = suspendSettingsService.getUnlockPrice();
                activityBlockRepository.findActiveByTargetForUpdate(user)
                        .ifPresentOrElse(
                                existing -> existing.reassignAsAdminSuspension(actingUser, price, suspendedUntil),
                                () -> activityBlockRepository.save(
                                        ActivityBlock.createByAdminSuspend(user, actingUser, price, suspendedUntil))
                        );
                refreshTokenRepository.deleteByUserId(userId);
                return;
            }

            user.suspend(suspendedUntil);
            refreshTokenRepository.deleteByUserId(userId);
        } else {
            user.activate();
            // ADMIN이 건 정지(=ActivityBlock으로 대체된 것)가 남아있으면 함께 풀어준다. item이 있는
            // (실제로 구매된) 차단은 건드리지 않는다 - 그건 unblockSelf/이의제기로만 풀어야 한다.
            activityBlockRepository.findActiveByTargetForUpdate(user)
                    .filter(block -> block.getItem() == null)
                    .ifPresent(ActivityBlock::lift);
        }
    }

    @Transactional
    public void updateGrade(Long userId, UserGrade grade) {
        User user = findById(userId);
        validateWithdrawUser(user);

        if (grade == UserGrade.SHOGUN && user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.SHOGUN_REQUIRES_ADMIN_ROLE);
        }

        user.updateGrade(grade);
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

        // 경매로 얻은 role이 아직 활성 상태로 남아있으면, 이 수동 변경을 만료 스케줄러
        // (RoleAuctionService.expireRolePurchases)가 나중에 조용히 되돌려버릴 수 있다 - 함께
        // 만료 처리해서 그 경매 건을 무효화한다.
        rolePurchaseRepository.findByUserAndStatus(user, RolePurchaseStatus.ACTIVE)
                .ifPresent(RolePurchase::expire);

        user.updateRole(role);
    }

    // ROOT가 ADMIN 권한을 즉시 박탈한다. 경매로 얻은 권한이면 그 RolePurchase를 즉시 만료 처리해
    // 스케줄러가 나중에 다시 손대지 않게 하고 previousRole로 되돌린다. 수동으로 임명된 ADMIN이면
    // (RolePurchase가 없으므로) 그냥 USER로 되돌린다. JwtAuthenticationFilter가 매 요청마다 role을
    // DB에서 다시 확인하므로, 이 변경은 대상의 다음 요청부터 곧바로(로그인 세션 종료 없이) 반영된다.
    @Transactional
    public void revokeAdmin(Long userId) {
        User user = findById(userId);

        if (user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.TARGET_NOT_ADMIN);
        }

        rolePurchaseRepository.findByUserAndStatus(user, RolePurchaseStatus.ACTIVE)
                .ifPresentOrElse(
                        purchase -> {
                            purchase.expire();
                            user.updateRole(purchase.getPreviousRole());
                        },
                        () -> user.updateRole(UserRole.USER)
                );
    }

    // ========== private 메서드 ==========

    private void validateWithdrawUser(User user) {
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }
    }

}