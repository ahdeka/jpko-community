package com.jpkocommunity.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;


@Component
public class CookieUtils {

    @Value("${cookie.secure}")
    private boolean secure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Value("${cookie.domain}")
    private String domain;

    // 쿠키 발급
    public void add(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge);

        if (!domain.isBlank()) {
            builder.domain(domain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void delete(HttpServletResponse response, String name) {
        add(response, name, "", 0);
    }

    // 쿠키 존재 여부
    public boolean exists(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return false;
        }
        return Arrays.stream(request.getCookies())
                .anyMatch(c -> c.getName().equals(name));
    }

    // 쿠키 값 추출
    public Optional<String> extract(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }
}
