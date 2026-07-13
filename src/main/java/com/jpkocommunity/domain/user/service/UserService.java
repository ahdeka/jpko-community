package com.jpkocommunity.domain.user.service;

import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.dto.request.UpdateNicknameRequest;
import com.jpkocommunity.domain.user.dto.request.UpdatePasswordRequest;
import com.jpkocommunity.domain.user.dto.request.WithdrawRequest;
import com.jpkocommunity.domain.user.dto.response.AdminUserResponse;
import com.jpkocommunity.domain.user.dto.response.MyCommentResponse;
import com.jpkocommunity.domain.user.dto.response.MyPostResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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