package com.jpkocommunity.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpkocommunity.domain.auth.repository.UserAuthProjection;
import com.jpkocommunity.domain.shop.entity.BlockStatus;
import com.jpkocommunity.domain.shop.repository.ActivityBlockRepository;
import com.jpkocommunity.domain.user.dto.response.SuspendedAccountResponse;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.BlockAllowedPaths;
import com.jpkocommunity.global.security.OnboardingAllowedPaths;
import com.jpkocommunity.global.security.PublicAuthPaths;
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
import java.time.LocalDateTime;
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
    private final UserRepository userRepository;
    private final ActivityBlockRepository activityBlockRepository;

    // 온보딩전 계정들은 읽기만 허용 TODO 마이그레이션 끝나면 정리
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    // EXCLUDED_PATHS 요청은 필터 자체를 건너뜀
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicAuthPaths.PATHS.contains(request.getRequestURI());
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

            // 매 요청마다 실시간 유저 상태 확인. role도 토큰에 박힌 로그인 시점 값이 아니라
            // 여기서 DB로 다시 확인한 최신값을 쓴다 — 그래야 권한 변경(경매 만료, ROOT의 즉시
            // 박탈 등)이 토큰 만료(최대 1시간)를 기다리지 않고 바로 다음 요청부터 반영된다.
            UserAuthProjection auth = userRepository.findAuthProjectionById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ONBOARDING_INCOMPLETE));
            UserRole role = auth.getRole();

            if (auth.getStatus() != UserStatus.ACTIVE) {
                throw new CustomException(ErrorCode.SUSPENDED_ACCOUNT, new SuspendedAccountResponse(auth.getSuspendedUntil()));
            }

            // 임시 활동 차단 확인 — 만료 시각을 실시간 비교하므로 스케줄러 없이도 만료 즉시 자동 해제됨
            boolean isBlocked = activityBlockRepository.existsByTarget_IdAndStatusAndExpiresAtAfter(
                    userId, BlockStatus.ACTIVE, LocalDateTime.now());
            if (isBlocked && !BlockAllowedPaths.PATHS.contains(request.getRequestURI())) {
                throw new CustomException(ErrorCode.ACTIVITY_BLOCKED);
            }

            // 온보딩 완료 판정 TODO 마이그레이션 끝나면 정리
            boolean fullyOnboarded = auth.getLoginId() != null && auth.isEmailVerified();
            boolean isWriteMethod = WRITE_METHODS.contains(request.getMethod());

            if (!fullyOnboarded && isWriteMethod && !OnboardingAllowedPaths.PATHS.contains(request.getRequestURI())) {
                throw new CustomException(ErrorCode.ONBOARDING_INCOMPLETE);
            }

            // 클레임으로 인증 객체 생성 (상태는 위에서 DB 확인)
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
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(e.getErrorCode(), e.getData()));
    }
}