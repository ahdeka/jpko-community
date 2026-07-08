package com.jpkocommunity.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 오프셋 없는 LocalDateTime 값에 대해
 * 프론트 시각을 KST(+09:00)로 변환하여 응답하도록 Jackson 설정
 */
@Configuration
public class JacksonConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 모든 LocalDateTime 응답에 +09:00 오프셋을 붙여줌
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer kstLocalDateTimeCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeString(
                        value.atZone(KST).toOffsetDateTime()
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
        return builder -> builder.modulesToInstall(module);
    }
}
