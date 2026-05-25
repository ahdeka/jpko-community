package com.jpkocommunity.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    // refresh: 만료된 accessToken으로 요청이 오므로 필터에서 제외 필수
    // login/signup: 토큰 없어도 무해하지만 명시적으로 제외
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/refresh"
    );

    // EXCLUDED_PATHS 요청은 필터 자체를 건너뜀
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.contains(path);
    }

    // 토큰 추출 → 검증 → SecurityContext 등록 흐름
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        // 토큰 없으면 인증 없이 통과
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtProvider.getUserId(token);
            UserRole role = jwtProvider.getRole(token);

            // DB 조회 없이 토큰 클레임만으로 인증 객체 생성
            AuthUser authUser = new AuthUser(userId, role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authUser,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                    );

            // IP, 세션ID 등 요청 부가정보 저장 (어뷰징 차단 시 활용)
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 인증 성공 - userId: {}, role: {}", userId, role);

        } catch (CustomException e) {
            log.warn("JWT 인증 실패 - uri: {}, error: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 추출 우선순위: 쿠키(웹) → Authorization 헤더(API)
     * HttpOnly 쿠키는 JS 접근 불가 → XSS 토큰 탈취 방어
     */
    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    // JWT 예외를 ApiResponse 형식의 JSON으로 변환해 응답
    private void sendErrorResponse(HttpServletResponse response, CustomException e) throws IOException {
        response.setStatus(e.getErrorCode().getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(e.getErrorCode()));
    }
}