package com.jpkocommunity.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
public class IpUtils {

    private static final String UNKNOWN = "알 수 없음";
    private static final Set<String> IPV6_LOOPBACKS = Set.of("::1", "0:0:0:0:0:0:0:1"); // IPv6 루프백 따로 처리
    private static final String IPV4_MAPPED_PREFIX = "::ffff:"; // IPv6로 감싸진 IPv4

    public String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // nginx가 검증된 IP 하나를 넘겨주긴 하는데, 방어적으로 첫 값만 사용
            return forwarded.split(",")[0].trim().toLowerCase();
        }
        return request.getRemoteAddr().toLowerCase();
    }

    public String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && !userAgent.isBlank()) ? userAgent : "unknown";
    }

    /**
     * 익명 표시용 마스킹
     * IPv4, IPv6 각각 앞 두자리
     */
    public static String mask(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) return UNKNOWN;
        String ip = ipAddress.trim().toLowerCase();
        if (IPV6_LOOPBACKS.contains(ip)) return "127.0";
        // IPv4-mapped IPv6는 IPv4 규칙으로 처리
        if (ip.startsWith(IPV4_MAPPED_PREFIX)) {
            ip = ip.substring(IPV4_MAPPED_PREFIX.length());
        }
        return ip.contains(".") ? maskIpv4(ip) : maskIpv6(ip);
    }

    private static String maskIpv4(String ip) {
        String[] octets = ip.split("\\.");
        return octets.length >= 2 ? octets[0] + "." + octets[1] : UNKNOWN;
    }

    private static String maskIpv6(String ip) {
        // ::abcd 처럼 앞이 생략된 주소는 앞 2그룹이 모두 0
        if (ip.startsWith("::")) return "0:0";
        String[] groups = ip.split(":");
        // 잘못된 데이터(콜론이나 점도 없음)는 여기서 걸러짐
        if (groups.length < 2) return UNKNOWN;
        return trimLeadingZeros(groups[0]) + ":" + trimLeadingZeros(groups[1]);
    }

    // 표기 통일 메서드
    private static String trimLeadingZeros(String hextet) {
        String trimmed = hextet.replaceFirst("^0+", "");
        return trimmed.isEmpty() ? "0" : trimmed;
    }
}
