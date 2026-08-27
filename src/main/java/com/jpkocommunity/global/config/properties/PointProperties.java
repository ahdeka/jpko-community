package com.jpkocommunity.global.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class PointProperties {
    private Map<String, Integer> points;

    @PostConstruct
    void validate() {
        Integer limit = points == null ? null : points.get("daily-activity-limit");
        if (limit == null || limit < 0) {
            throw new IllegalStateException("app.points.daily-activity-limit 설정이 없거나 음수입니다: " + limit);
        }
    }

    public long dailyActivityLimit() {
        return points.get("daily-activity-limit");
    }
}
