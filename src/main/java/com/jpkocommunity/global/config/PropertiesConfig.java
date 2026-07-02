package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ConfigurationProperties를 활성화하기 위한 설정 클래스
 *  - JwtProperties를 @ConfigurationProperties로 등록
 *  - @EnableConfigurationProperties를 통해 JwtProperties를 스프링 컨테이너에 등록
 *  - JwtProperties는 application.yml의 jwt 설정을 바인딩
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class PropertiesConfig {
}