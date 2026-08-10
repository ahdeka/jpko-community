package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 포인트 랭킹 노출 인원 관리 (고정 정책 값)
 *      - topSize: 상위 랭킹 조회 시 노출할 인원 수 (비로그인)
 *      - neighborSize: 내 순위 조회 시 인근 랭킹 인원 수 (로그인)
 */
@ConfigurationProperties(prefix = "app.ranking")
public record RankingProperties(
        int topSize,
        int neighborSize
) {
}