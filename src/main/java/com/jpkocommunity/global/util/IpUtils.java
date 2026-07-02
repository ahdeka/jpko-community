package com.jpkocommunity.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;


@Component
public class IpUtils {

    public String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 첫 번째 IP 주소를 원본으로 사용
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && !userAgent.isBlank()) ? userAgent : "unknown";
    }
}
