package com.jpkocommunity.domain.auth.service;

import com.jpkocommunity.domain.auth.dto.request.EmailChangeRequest;
import com.jpkocommunity.domain.auth.dto.request.LoginRequest;
import com.jpkocommunity.domain.auth.dto.request.SignupCompleteRequest;
import com.jpkocommunity.domain.auth.dto.request.SignupInitRequest;
import com.jpkocommunity.domain.auth.dto.response.LoginResponse;
import com.jpkocommunity.domain.auth.dto.response.PreSignupVerifyResponse;
import com.jpkocommunity.domain.auth.entity.RefreshToken;
import com.jpkocommunity.domain.auth.entity.VerificationToken;
import com.jpkocommunity.domain.auth.entity.VerificationTokenType;
import com.jpkocommunity.domain.auth.event.FoundAccountEmailEvent;
import com.jpkocommunity.domain.auth.event.VerificationEmailEvent;
import com.jpkocommunity.domain.auth.repository.RefreshTokenRepository;
import com.jpkocommunity.domain.auth.repository.VerificationTokenRepository;
import com.jpkocommunity.domain.level.service.LevelService;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.global.config.VerificationTokenProperties;
import com.jpkocommunity.global.config.properties.JwtProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.security.jwt.JwtProvider;
import com.jpkocommunity.global.util.EmailDomainValidator;
import com.jpkocommunity.global.util.NicknamePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final LevelService levelService;

    // TODO: 이메일 반복 요청 방어 처리 필요

    /**
     * 회원가입
     */
    @Transactional
    public void initSignup(SignupInitRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // TODO: 이 메서드는 회원가입 트랜잭션 안에 DNS 조회가 있는 문제임 (문제될시 처리)
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
                .expiresAt(LocalDateTime.now().plusSeconds(verificationTokenProperties.preSignupExpiration() / 1000))
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

    /**
     * 회원가입 완료
     */
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

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // validate 3개를 뚫고온 후이므로, 이 예외는 "동시 요청 선점" 밖에 없음
            throw new CustomException(resolveDuplicate(e));
        }

        levelService.createUserLevel(user);
        verificationTokenRepository.delete(verificationToken);

        TokenPair tokens = issueTokens(user, deviceInfo, ipAddress);
        return toLoginResult(user.getId(), user, tokens);
    }

    private ErrorCode resolveDuplicate(DataIntegrityViolationException e) {
        String cause = String.valueOf(e.getMostSpecificCause().getMessage());
        if (cause.contains("uk_users_login_id")) return ErrorCode.DUPLICATE_LOGIN_ID;
        if (cause.contains("uk_users_email")) return ErrorCode.DUPLICATE_EMAIL;
        if (cause.contains("uk_users_nickname_ci")) return ErrorCode.DUPLICATE_NICKNAME;

        // unique 위반이 아닌 무결성일 경우, 로그를 남기고 일반 오류로 처리
        log.error("[회원가입] 예상치 못한 무결성 위반", e);
        return ErrorCode.INTERNAL_ERROR;
    }

    @Transactional
    public void requestEmailChange(Long userId, EmailChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.email().equals(user.getEmail())) {
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
                .expiresAt(LocalDateTime.now().plusSeconds(verificationTokenProperties.emailVerificationExpiration() / 1000))
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
    public LoginResult login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new CustomException(ErrorCode.WRONG_PASSWORD));

        validateLoginInfo(user, request);
        TokenPair tokens = issueTokens(user, deviceInfo, ipAddress);

        return toLoginResult(user.getId(), user, tokens);
    }

    private LoginResult toLoginResult(Long userId, User user, TokenPair tokens) {
        return new LoginResult(
                userId,
                new LoginResponse(user.getNickname(), user.getRole()),
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
        if (NicknamePolicy.isBlackList(request.nickname())) {
            throw new CustomException(ErrorCode.BLACKLIST_NICKNAME);
        }
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByNicknameIgnoreCase(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // SUSPENDED는 여기서 막지 않는다 — 정지된 계정도 로그인은 되게 해서 출첵이 정상 처리되고,
    // 포인트로 직접 정지를 해제할 수 있게 한다(SuspendAllowedPaths로 제한된 화면만 쓸 수 있음).
    // 실제 접근 제한은 JwtAuthenticationFilter가 매 요청마다 담당한다.
    private void validateLoginInfo(User user, LoginRequest request) {
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }
    }

    // ========== 아이디/비밀번호 찾기 ==========
    @Transactional
    public void findAccount(String email, String ipAddress) {
        userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .ifPresentOrElse(
                        user -> publishRecoveryMail(user, ipAddress),
                        () -> publishNotFoundMail(email, ipAddress)
                );
    }

    private void publishRecoveryMail(User user, String ipAddress) {
        String token = issueToken(user.getId(), VerificationTokenType.PASSWORD_RESET,
                verificationTokenProperties.passwordResetExpiration());
        eventPublisher.publishEvent(
                FoundAccountEmailEvent.found(user.getEmail(), user.getLoginId(), token, ipAddress));
    }
    private void publishNotFoundMail(String email, String ipAddress) {
        if (!emailDomainValidator.hasMxRecord(email)) {
            return;
        }
        eventPublisher.publishEvent(FoundAccountEmailEvent.notFound(email, ipAddress));
    }

    // ========== 비밀번호 재설정 ==========

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
