package com.jpkocommunity.domain.point.dto.response;

/**
 * 네이티브 쿼리 결과 매핑용 projection
 *   - 쿼리 결과가 DTO로 매핑되게
 */
public interface RankingRow {
    Long getUserId();
    String getNickname();
    Long getBalance();
}