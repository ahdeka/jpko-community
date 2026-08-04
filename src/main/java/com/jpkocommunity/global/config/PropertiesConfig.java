package com.jpkocommunity.global.config;

import com.jpkocommunity.global.config.properties.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ConfigurationProperties를 활성화하기 위한 설정 클래스
 * - @EnableConfigurationProperties를 통해
 *      아래에 선언한 class를 스프링 컨테이너에 등록
 */
@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        VerificationTokenProperties.class,
        MailProperties.class,
        ThrottleProperties.class,
        ImageProcessingProperties.class,
        TurnstileProperties.class,
        CheckInProperties.class,
        RouletteProperties.class,
        PointProperties.class,
        TransferProperties.class
})
public class PropertiesConfig {
}