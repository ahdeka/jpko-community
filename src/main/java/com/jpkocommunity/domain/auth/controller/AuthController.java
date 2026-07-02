package com.jpkocommunity.domain.auth.controller;

import com.jpkocommunity.domain.auth.dto.request.LoginRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        // device_info: User-Agent 헤더, ip_address: X-Forwarded-For 또는 RemoteAddr
        String deviceInfo = Optional.ofNullable(servletRequest.getHeader("User-Agent")).orElse("unknown");
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

}
