package com.jpkocommunity.domain.auth.controller;

import com.jpkocommunity.domain.auth.dto.request.LoginRequest;
import com.jpkocommunity.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.jpkocommunity.domain.auth.dto.request.PasswordResetRequest;
import com.jpkocommunity.domain.auth.dto.request.SignupRequest;
import com.jpkocommunity.domain.auth.dto.response.LoginResponse;
import com.jpkocommunity.domain.auth.dto.response.UserInfoResponse;
import com.jpkocommunity.domain.auth.service.AuthService;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.JwtProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import com.jpkocommunity.global.util.CookieUtils;
import com.jpkocommunity.global.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CookieUtils cookieUtils;
    private final IpUtils ipUtils;
    private final JwtProperties jwtProperties;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> me(@AuthenticationPrincipal AuthUser authUser) {
        User user = userService.findById(authUser.userId());
        return ResponseEntity.ok(ApiResponse.ok(
                new UserInfoResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole())
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        String deviceInfo = ipUtils.getDeviceInfo(servletRequest);
        String ipAddress = ipUtils.getClientIp(servletRequest);

        AuthService.LoginResult result = authService.signup(request, deviceInfo, ipAddress);

        cookieUtils.add(response, "accessToken", result.accessToken(), jwtProperties.accessTokenMaxAgeSeconds());
        cookieUtils.add(response, "refreshToken", result.refreshToken(), jwtProperties.refreshTokenMaxAgeSeconds());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입이 완료되었습니다.", result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String deviceInfo = ipUtils.getDeviceInfo(servletRequest);
        String ipAddress = ipUtils.getClientIp(servletRequest);

        AuthService.LoginResult result = authService.login(request, deviceInfo, ipAddress);

        cookieUtils.add(response, "accessToken", result.accessToken(), jwtProperties.accessTokenMaxAgeSeconds());
        cookieUtils.add(response, "refreshToken", result.refreshToken(), jwtProperties.refreshTokenMaxAgeSeconds());

        return ResponseEntity.ok(ApiResponse.ok(result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = cookieUtils.extract(request, "refreshToken")
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        String newAccessToken = authService.refresh(refreshToken);

        cookieUtils.add(response, "accessToken", newAccessToken, jwtProperties.accessTokenMaxAgeSeconds());
        return ResponseEntity.ok(ApiResponse.ok("토큰이 갱신되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        cookieUtils.extract(request, "refreshToken").ifPresent(authService::logout);
        cookieUtils.delete(response, "accessToken");
        cookieUtils.delete(response, "refreshToken");
        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 완료되었습니다."));
    }

    @PostMapping("/email-verification/request")
    public ResponseEntity<ApiResponse<Void>> requestEmailVerification(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        authService.sendVerificationEmail(authUser.userId());
        return ResponseEntity.ok(ApiResponse.ok("인증 메일이 발송되었습니다."));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.resendVerificationEmailByEmail(request.email());
        return ResponseEntity.ok(ApiResponse.ok(
                "입력하신 이메일이 가입되어 있고 미인증 상태라면, 인증 메일을 보내드렸습니다."
        ));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @RequestParam String token
    ) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.ok(
                "입력하신 이메일이 가입되어 있고 인증된 상태라면, 재설정 링크를 보내드렸습니다." +
                        "메일이 오지 않는다면 이메일 인증이 필요한 상태일 수 있습니다."
        ));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        authService.resetPassword(request.token(), request.newPassword(), request.newPasswordConfirm());
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 재설정되었습니다."));
    }

}
