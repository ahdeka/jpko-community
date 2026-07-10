package com.jpkocommunity.domain.auth.service;

import com.jpkocommunity.domain.auth.dto.request.LoginRequest;
import com.jpkocommunity.domain.auth.dto.request.SignupRequest;
import com.jpkocommunity.domain.auth.dto.response.LoginResponse;
import com.jpkocommunity.domain.auth.entity.RefreshToken;
import com.jpkocommunity.domain.auth.event.VerificationEmailEvent;
import com.jpkocommunity.domain.auth.entity.VerificationToken;
import com.jpkocommunity.domain.auth.entity.VerificationTokenType;
import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.auth.repository.VerificationTokenRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.global.config.JwtProperties;
import com.jpkocommunity.global.config.VerificationTokenProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.security.jwt.JwtProvider;
import com.jpkocommunity.global.util.EmailDomainValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final EmailDomainValidator emailDomainValidator;
    private final VerificationTokenProperties verificationTokenProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoginResult signup(SignupRequest request, String deviceInfo, String ipAddress) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        userRepository.save(user);

        return issueTokens(user, deviceInfo, ipAddress);
    }

    @Transactional
    public LoginResult login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.WRONG_PASSWORD));

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        return issueTokens(user, deviceInfo, ipAddress);
    }

    // 토큰 발급 공통 로직
    private LoginResult issueTokens(User user, String deviceInfo, String ipAddress) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        // 같은 기기 재로그인 시 기존 토큰만 교체
        refreshTokenRepository.deleteByUserIdAndDeviceInfo(user.getId(), deviceInfo);
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenMaxAgeSeconds()))
                .build());

        return new LoginResult(
                new LoginResponse(user.getNickname(), user.getRole()),
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public String refresh(String refreshToken) {
        // 1. DB에 존재하는 토큰인지 확인 (탈취 후 로그아웃된 토큰 차단)
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        // 2. 만료 여부 확인
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        // 3. 유저 조회 후 새 accessToken 발급
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return jwtProvider.generateAccessToken(user.getId(), user.getRole());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    // ========== 헬퍼 메서드 ===========

    // 토큰 저장까지만 트랜잭션 안에서 수행 (발송은 이벤트로 커밋 후 비동기 처리)
    private String issueToken(Long userId, VerificationTokenType type, long expirationMillis) {
        verificationTokenRepository.deleteByUserIdAndType(userId, type);

        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(VerificationToken.builder()
                .userId(userId)
                .token(token)
                .type(type)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationMillis / 1000))
                .build());
        return token;
    }

    // ========== 이메일 인증 ==========

    @Transactional
    public void sendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED_EMAIL);
        }

        // 최초 인증 요첨 시점에만 MX 레코드 체크
        if (!emailDomainValidator.hasMxRecord(user.getEmail())) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        String token = issueToken(user.getId(), VerificationTokenType.EMAIL_VERIFICATION,
                verificationTokenProperties.emailVerificationExpiration());


        // 이메일 전송은 이벤트로 처리하여 비동기화 (트랜잭션과 분리)
        eventPublisher.publishEvent(
                new VerificationEmailEvent(user.getEmail(), token, VerificationTokenType.EMAIL_VERIFICATION));
    }

    @Transactional
    public void resendVerificationEmailByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }

            if (!emailDomainValidator.hasMxRecord(email)) {
                return;
            }

            String token = issueToken(user.getId(), VerificationTokenType.EMAIL_VERIFICATION,
                    verificationTokenProperties.emailVerificationExpiration());

            eventPublisher.publishEvent(
                    new VerificationEmailEvent(user.getEmail(), token, VerificationTokenType.EMAIL_VERIFICATION));
        });
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .filter(t -> t.getType() == VerificationTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_TOKEN));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.verifyEmail();
        verificationTokenRepository.delete(verificationToken);
    }

    // ========== 비밀번호 재설정 ==========

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                return;
            }

            // 비밀번호 재설정은 MX 재확인 없음
            String token = issueToken(user.getId(), VerificationTokenType.PASSWORD_RESET,
                    verificationTokenProperties.passwordResetExpiration());

            eventPublisher.publishEvent(
                    new VerificationEmailEvent(user.getEmail(), token, VerificationTokenType.PASSWORD_RESET));
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .filter(t -> t.getType() == VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_TOKEN));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(newPassword));
        verificationTokenRepository.delete(verificationToken);

        // 다른 모든 기기의 세션을 강제 로그아웃
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    public record LoginResult(LoginResponse response, String accessToken, String refreshToken) { }
}
