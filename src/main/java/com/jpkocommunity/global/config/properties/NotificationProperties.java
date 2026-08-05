package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 알림 보관 기간 관리
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        int retentionDays
) {}