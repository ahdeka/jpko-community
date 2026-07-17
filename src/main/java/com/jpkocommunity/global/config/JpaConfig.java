package com.jpkocommunity.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *  - @CreatedDate, @LastModifiedDate 등 JPA Auditing 어노테이션을 활성화하기 위한 설정 클래스
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}