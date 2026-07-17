package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ConfigurationProperties를 활성화하기 위한 설정 클래스
 * - @EnableConfigurationProperties를 통해 JwtProperties를 스프링 컨테이너에 등록
 * - @EnableConfigurationProperties를 통해 VerificationTokenProperties를 스프링 컨테이너에 등록
 * - @EnableConfigurationProperties를 통해 MailProperties를 스프링 컨테이너에 등록
 * - @EnableConfigurationProperties를 통해 ThrottleProperties 스프링 컨테이너에 등록
 */
@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        VerificationTokenProperties.class,
        MailProperties.class,
        ThrottleProperties.class
})
public class PropertiesConfig {
}