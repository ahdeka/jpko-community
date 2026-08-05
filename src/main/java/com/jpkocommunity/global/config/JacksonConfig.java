package com.jpkocommunity.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * LocalDateTime <-> KST(+09:00) 변환을 양방향으로 맞춰주는 Jackson 설정.
 * 직렬화만 있고 역직렬화가 없으면, 클라이언트가 UTC("Z")로 보낸 시각을 Jackson 기본
 * LocalDateTimeDeserializer가 시간대 변환 없이 숫자만 그대로 읽어 9시간이 어긋난다
 * (정지 기한(suspendedUntil) 등 클라이언트가 보내는 시각 필드에서 실제로 발생한 버그).
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
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getValueAsString();
                if (!StringUtils.hasText(text)) {
                    return null;
                }
                // 오프셋/Z가 있는 문자열(예: "...Z", "...+09:00")은 같은 시점의 KST 벽시계로 변환한다.
                // 오프셋이 없는 순수 로컬 문자열(우리 자신이 직렬화한 형태 그대로 되돌아오는 경우 등)은 그대로 파싱한다.
                try {
                    return OffsetDateTime.parse(text).atZoneSameInstant(KST).toLocalDateTime();
                } catch (DateTimeParseException e) {
                    return LocalDateTime.parse(text);
                }
            }
        });
        return builder -> builder.modulesToInstall(module);
    }
}
