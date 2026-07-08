package com.jpkocommunity.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * JPA AuditEntityListener에서 LocalDateTime을 사용할 때
 * JVM 기본값 UTC 말고, 서울 (KST, UTC+9)로 고정
 */
@Configuration
public class TimeZoneConfig {

    // 컨테이너 기본값(UTC) 대신 서울(KST, UTC+9)로 고정
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
