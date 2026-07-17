package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 메일 관련 설정
 *  from: 발송자 이메일 주소
 *  fromName: 발송자 이름
 *  baseUrl: 애플리케이션 기본 URL
 */
@ConfigurationProperties(prefix = "mail")
public record MailProperties(
        String from,
        String fromName,
        String baseUrl
) {}