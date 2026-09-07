package com.jpkocommunity.domain.auth.controller;

import com.jpkocommunity.domain.auth.dto.request.*;
import com.jpkocommunity.domain.auth.dto.response.LoginResponse;
import com.jpkocommunity.domain.auth.dto.response.PreSignupVerifyResponse;
import com.jpkocommunity.domain.auth.dto.response.UserInfoResponse;
import com.jpkocommunity.domain.auth.service.AuthService;
import com.jpkocommunity.domain.auth.service.TurnstileService;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.JwtProperties;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CookieUtils cookieUtils;
    private final IpUtils ipUtils;
    private final JwtProperties jwtProperties;
    private final TurnstileService turnstileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> me(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyInfo(authUser.userId())));
    }

    @PostMapping("/signup/init")
    public ResponseEntity<ApiResponse<Void>> initSignup(
            @Valid @RequestBody SignupInitRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = ipUtils.getClientIp(servletRequest);
        turnstileService.verify(request.turnstileToken(), ipAddress);
        authService.initSignup(request);

        return ResponseEntity.ok(ApiResponse.ok("인증 메일을 보냈습니다. 메일에서 나머지 절차를 진행해주세요."));
    }

    @GetMapping("/signup/verify")
    public ResponseEntity<ApiResponse<PreSignupVerifyResponse>> verifySignupToken(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyPreSignupToken(token)));
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<ApiResponse<LoginResponse>> completeSignup(
            @Valid @RequestBody SignupCompleteRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String deviceInfo = ipUtils.getDeviceInfo(servletRequest);
        String ipAddress = ipUtils.getClientIp(servletRequest);

        AuthService.LoginResult result = authService.completeSignup(request, deviceInfo, ipAddress);

        cookieUtils.add(response, "accessToken", result.accessToken(), jwtProperties.accessTokenMaxAgeSeconds());
        cookieUtils.add(response, "refreshToken", result.refreshToken(), jwtProperties.refreshTokenMaxAgeSeconds());

        LoginResponse.CheckInInfo checkIn = authService.checkInAfterSignup(result.userId());
        LoginResponse body = new LoginResponse(
                result.response().nickname(),
                result.response().role(),
                checkIn);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입이 완료되었습니다.", body));
    }

    @PatchMapping("/email")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody EmailChangeRequest request
    ) {
        authService.requestEmailChange(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("입력하신 새 이메일로 인증 메일을 보냈습니다."));
    }

    @PostMapping("/email/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(
            @RequestParam String token
    ) {
        authService.confirmEmailChange(token);
        return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String ipAddress = ipUtils.getClientIp(servletRequest);
        turnstileService.verify(request.turnstileToken(), ipAddress);
        String deviceInfo = ipUtils.getDeviceInfo(servletRequest);

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

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.ok(
                "입력하신 이메일이 가입되어 있다면, 재설정 링크를 보내드렸습니다."
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
