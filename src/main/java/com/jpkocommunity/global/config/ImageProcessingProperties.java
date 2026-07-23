package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GIF -> WebP변환을 위한 외부 프로그램 경로 관리
 */
@ConfigurationProperties(prefix = "app.image")
public record ImageProcessingProperties(String gif2webpPath) {
}