package com.jpkocommunity.global.security.jwt;

import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.global.config.JwtProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    // JwtProperties를 주입받아 secretKey와 만료 시간을 초기화
    public JwtProvider(JwtProperties jwtProperties) {
        // UTF_8 명시: OS 환경에 따른 기본 인코딩 차이 방지
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = jwtProperties.accessTokenExpiration();
        this.refreshTokenExpiration = jwtProperties.refreshTokenExpiration();
    }

    /**
     * Access Token 생성
     * - subject: userId (토큰 소유자 식별)
     * - claim: role (권한 체크용, 매 요청 DB 조회 없이 사용)
     */
    public String generateAccessToken(Long userId, UserRole role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     * - role 미포함: 재발급용으로만 사용, 권한 정보 불필요
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public UserRole getRole(String token) {
        String role = parseClaims(token).get(ROLE_CLAIM, String.class);
        return UserRole.valueOf(role);
    }

    /**
     * 토큰 파싱 + 유효성 검증 통합 메서드
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN); // 만료 → 재발급 유도
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN); // 위조 및 변조  →  재로그인
        }
    }
}