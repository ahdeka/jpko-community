package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ConfigurationProperties를 활성화하기 위한 설정 클래스
 * - @EnableConfigurationProperties를 통해
 *      - JwtProperties를 스프링 컨테이너에 등록
 *      - VerificationTokenProperties를 스프링 컨테이너에 등록
 *      - MailProperties를 스프링 컨테이너에 등록
 *      - ThrottleProperties를 스프링 컨테이너에 등록
 *      - ImageProcessingProperties를 스프링 컨테이너에 등록
 *      - RouletteProperties를 스프링 컨테이너에 등록
 */
@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        VerificationTokenProperties.class,
        MailProperties.class,
        ThrottleProperties.class,
        ImageProcessingProperties.class,
        RouletteProperties.class
})
public class PropertiesConfig {
}