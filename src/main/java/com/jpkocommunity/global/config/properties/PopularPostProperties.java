package com.jpkocommunity.global.config.properties;

import com.jpkocommunity.domain.post.dto.request.PopularPeriod;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인기글 점수 산정 설정
 *
 *  점수 공식 = (좋아요수 * likeWeight + 댓글수 * commentWeight + ln(조회수 + 1) * viewWeight)
 *            / (경과시간(h) + 2) ^ gravity
 */
@ConfigurationProperties(prefix = "app.popular-post")
public record PopularPostProperties(
        double likeWeight,
        double commentWeight,
        double viewWeight,
        int displaySize,
        int refreshIntervalMinutes,
        Period today,
        Period weekly
) {

    /**
     * @param days              조회 기간(일)
     * @param gravity           시간 경과에 따른 점수 감소 정도 - 클수록 오래된 글이 빨리 밀려감
     * @param minEngagement     인기글 후보 진입 문턱 - 집계 기간 내(좋아요 + 댓글)수
     */
    public record Period(int days, double gravity, long minEngagement) {}

    public PopularPostProperties {
        // 컴팩트 생성자 안에서는 필드 초기화 되기전에 파라미터 검증 필요
        if (likeWeight < 0 || commentWeight < 0 || viewWeight < 0) {
            throw new IllegalArgumentException("app.popular-post의 가중치는 음수일 수 없습니다.");
        }
        if (displaySize <= 0) {
            throw new IllegalArgumentException("app.popular-post.display-size는 0보다 커야 합니다: " + displaySize);
        }
        if (refreshIntervalMinutes <= 0) {
            throw new IllegalArgumentException("app.popular-post.refresh-interval-minutes는 0보다 커야 합니다: " + refreshIntervalMinutes);
        }
        if (today == null || weekly == null) {
            throw new IllegalArgumentException("app.popular-post.today/weekly 설정이 없습니다.");
        }

        // 각 기간별 설정 검증
        for (PopularPeriod key : PopularPeriod.values()) {
            validatePeriod(key, switch (key) {
                case TODAY -> today;
                case WEEKLY -> weekly;
            });
        }
    }

    // ========== private 메서드 ==========

    private static void validatePeriod(PopularPeriod key, Period period) {
        String prefix = "app.popular-post." + key.name().toLowerCase();
        if (period.days() <= 0) {
            throw new IllegalArgumentException(prefix + ".days는 0보다 커야 합니다: " + period.days());
        }
        if (period.gravity() < 0) {
            throw new IllegalArgumentException(prefix + ".gravity는 음수일 수 없습니다: " + period.gravity());
        }
        if (period.minEngagement() < 0) {
            throw new IllegalArgumentException(prefix + ".min-engagement는 음수일 수 없습니다: " + period.minEngagement());
        }
    }
}
