package com.jpkocommunity.domain.auth.service;

import com.jpkocommunity.domain.auth.dto.request.*;
import com.jpkocommunity.domain.auth.dto.response.LoginResponse;
import com.jpkocommunity.domain.auth.dto.response.PreSignupVerifyResponse;
import com.jpkocommunity.domain.auth.entity.RefreshToken;
import com.jpkocommunity.domain.auth.event.VerificationEmailEvent;
import com.jpkocommunity.domain.auth.entity.VerificationToken;
import com.jpkocommunity.domain.auth.entity.VerificationTokenType;
import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.auth.repository.VerificationTokenRepository;
import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.point.service.CheckInService;
import com.jpkocommunity.domain.user.dto.response.SuspendedAccountResponse;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.global.config.properties.JwtProperties;
import com.jpkocommunity.global.config.VerificationTokenProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.security.jwt.JwtProvider;
import com.jpkocommunity.global.util.EmailDomainValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
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
    private final EmailDomainValidator emailDomainValidator;
    private final VerificationTokenProperties verificationTokenProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final TurnstileService turnstileService;
    private final CheckInService checkInService;

    // TODO: 이메일 반복 요청 방어 처리 필요
    @Transactional
    public void initSignup(SignupInitRequest request, String ipAddress) {
        turnstileService.verify(request.turnstileToken(), ipAddress);

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (!emailDomainValidator.hasMxRecord(request.email())) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        // 같은 이메일 재 요청 시 이전 토큰 무효화
        verificationTokenRepository.deleteByEmailAndType(request.email(), VerificationTokenType.PRE_SIGNUP);

        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(VerificationToken.builder()
                .email(request.email())
                .token(token)
                .type(VerificationTokenType.PRE_SIGNUP)
                .expiresAt(LocalDateTime.now().plusSeconds(verificationTokenProperties.preSignupExpiration()))
                .build());

        eventPublisher.publishEvent(new VerificationEmailEvent(request.email(), token, VerificationTokenType.PRE_SIGNUP));
    }

    @Transactional(readOnly = true)
    public PreSignupVerifyResponse verifyPreSignupToken(String token) {
        VerificationToken verificationToken = getPreSignupToken(token);

        // 읽기전용 트랜잭션이라 삭제는 안함
        if (verificationToken.isExpired()) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }

        return new PreSignupVerifyResponse(verificationToken.getEmail());
    }

    @Transactional
    public LoginResult completeSignup(SignupCompleteRequest request, String deviceInfo, String ipAddress) {
        VerificationToken verificationToken = getPreSignupToken(request.token());

        validateSignupInfo(verificationToken, request);

        User user = User.builder()
                .email(verificationToken.getEmail())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .loginId(request.loginId())
                .build();
        user.verifyEmail();

        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);

        TokenPair tokens = issueTokens(user, deviceInfo, ipAddress);
        return toLoginResult(user.getId(), user, tokens, CheckInOutcome.none());
    }

    @Transactional
    public void completeOnBoarding(Long userId, SignupMigrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateOnboarding(user, request);

        user.assignLoginId(request.loginId());
        user.updatePassword(passwordEncoder.encode(request.password()));
    }

    @Transactional
    public void requestEmailChange(Long userId, EmailChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.email().equals(user.getEmail()) && user.isEmailVerified()) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED_EMAIL);
        }
        if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (!emailDomainValidator.hasMxRecord(request.email())) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        verificationTokenRepository.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CHANGE);

        String token = UUID.randomUUID().toString();

        verificationTokenRepository.save(VerificationToken.builder()
                .userId(userId)
                .email(request.email()) // 변경할 이메일을 토큰에 보관
                .token(token)
                .type(VerificationTokenType.EMAIL_CHANGE)
                .expiresAt(LocalDateTime.now().plusSeconds(verificationTokenProperties.emailVerificationExpiration()))
                .build());

        eventPublisher.publishEvent(
                new VerificationEmailEvent(
                        request.email(),
                        token,
                        VerificationTokenType.EMAIL_CHANGE)
        );
    }

    @Transactional
    public void confirmEmailChange(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .filter(t -> t.getType() == VerificationTokenType.EMAIL_CHANGE)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_TOKEN));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }

        if (userRepository.existsByEmailAndIdNot(verificationToken.getEmail(), verificationToken.getUserId())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.changeEmail(verificationToken.getEmail());
        verificationTokenRepository.delete(verificationToken);
    }

    @Transactional
    public LoginResult signup(SignupRequest request, String deviceInfo, String ipAddress) {
        turnstileService.verify(request.turnstileToken(), ipAddress);

        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
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

        // 출첵은 이 메서드가 커밋된 뒤 별도로 처리한다(checkInAfterSignup 참고).
        // 여기서 신규 유저를 대상으로 곧바로 출첵을 걸면, 출첵이 REQUIRES_NEW로 별도 트랜잭션에서
        // 실행되는 탓에 아직 커밋되지 않은 이 유저 row를 보지 못해 USER_NOT_FOUND로 실패한다.
        TokenPair tokens = issueTokens(user, deviceInfo, ipAddress);
        return toLoginResult(user.getId(), user, tokens, CheckInOutcome.none());
    }

    @Transactional
    public LoginResult login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByLoginId(request.loginId())
                .or(() -> userRepository.findByEmail(request.loginId()))
                .orElseThrow(() -> new CustomException(ErrorCode.WRONG_PASSWORD));

        validateLoginInfo(user, request);

        // 토큰 발급을 먼저 끝낸 뒤에 출첵을 처리한다. 반대 순서(출첵 먼저)로 하면
        // 출첵은 이미 커밋됐는데 토큰 저장이 실패해 로그인 자체는 실패하는 불일치가 생길 수 있다.
        // 이 순서면 토큰 발급이 실패할 경우 출첵은 아예 시도되지 않고, 출첵이 실패해도 로그인은 이미 발급된 상태다.
        TokenPair tokens = issueTokens(user, deviceInfo, ipAddress);
        CheckInOutcome checkInOutcome = checkInService.checkInIfEligible(user.getId());

        return toLoginResult(user.getId(), user, tokens, checkInOutcome);
    }

    // 회원가입 트랜잭션이 커밋된 뒤(AuthController가 signup() 호출에서 리턴받은 다음) 호출해야 한다.
    // 실패해도 회원가입 응답 자체는 깨지지 않도록 여기서 흡수한다.
    public LoginResponse.CheckInInfo checkInAfterSignup(Long userId) {
        try {
            return LoginResponse.CheckInInfo.from(checkInService.checkInIfEligible(userId));
        } catch (Exception e) {
            log.warn("[출첵] 회원가입 직후 출첵 처리 실패. userId={}", userId, e);
            return LoginResponse.CheckInInfo.from(CheckInOutcome.none());
        }
    }

    private LoginResult toLoginResult(Long userId, User user, TokenPair tokens, CheckInOutcome checkInOutcome) {
        return new LoginResult(
                userId,
                new LoginResponse(
                        user.getNickname(),
                        user.getRole(),
                        LoginResponse.CheckInInfo.from(checkInOutcome),
                        !user.isFullyOnboarded()
                ),
                tokens.accessToken(),
                tokens.refreshToken()
        );
    }

    // 토큰 발급 공통 로직
    private TokenPair issueTokens(User user, String deviceInfo, String ipAddress) {
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

        return new TokenPair(accessToken, refreshToken);
    }

    private record TokenPair(String accessToken, String refreshToken) {
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

    private VerificationToken getPreSignupToken(String token) {
        return verificationTokenRepository.findByToken(token)
                .filter(t -> t.getType() == VerificationTokenType.PRE_SIGNUP)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_TOKEN));
    }

    private void validateSignupInfo(VerificationToken verificationToken, SignupCompleteRequest request) {
        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }
        if (userRepository.existsByEmail(verificationToken.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateLoginInfo(User user, LoginRequest request) {
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.SUSPENDED_ACCOUNT, new SuspendedAccountResponse(user.getSuspendedUntil()));
        }
    }

    private void validateOnboarding(User user, SignupMigrationRequest request) {
        if (!user.isEmailVerified()) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        if (user.getLoginId() != null) {
            throw new CustomException(ErrorCode.ALREADY_ASSIGN_LOGIN_ID);
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (userRepository.existsByLoginId(request.loginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
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

    public record LoginResult(Long userId, LoginResponse response, String accessToken, String refreshToken) {
    }
}
