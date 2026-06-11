package com.jpkocommunity.domain.auth.controller;

import com.jpkocommunity.domain.auth.dto.request.LoginRequest;
import com.jpkocommunity.domain.auth.dto.request.SignupRequest;
import com.jpkocommunity.domain.auth.dto.response.LoginResponse;
import com.jpkocommunity.domain.auth.dto.response.UserInfoResponse;
import com.jpkocommunity.domain.auth.service.AuthService;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int ACCESS_TOKEN_MAX_AGE = 60 * 60;           // 1시간
    private static final int REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 7; // 7일

    private final AuthService authService;
    private final UserService userService;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

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
        String deviceInfo = servletRequest.getHeader("User-Agent");
        String ipAddress  = getClientIp(servletRequest);

        AuthService.LoginResult result = authService.login(request, deviceInfo, ipAddress);

        addCookie(response, "accessToken",  result.accessToken(),  ACCESS_TOKEN_MAX_AGE);
        addCookie(response, "refreshToken", result.refreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ResponseEntity.ok(ApiResponse.ok(result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookie(request, "refreshToken");
        String newAccessToken = authService.refresh(refreshToken);

        addCookie(response, "accessToken", newAccessToken, ACCESS_TOKEN_MAX_AGE);
        return ResponseEntity.ok(ApiResponse.ok("토큰이 갱신되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        extractCookieOptional(request, "refreshToken").ifPresent(authService::logout);
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");
        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 완료되었습니다."));
    }

    // ========== 쿠키 유틸 메서드 ==========

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> extractCookieOptional(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        return extractCookieOptional(request, name)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    // 프록시(Nginx, AWS ALB) 뒤에서도 실제 클라이언트 IP 추출
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // 여러 프록시 경유 시 첫 번째가 원본 IP
        }
        return request.getRemoteAddr();
    }
}
