package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public record MailProperties(
        String from,
        String fromName,
        String baseUrl
) {}