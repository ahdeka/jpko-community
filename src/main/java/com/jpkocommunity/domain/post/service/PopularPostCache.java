package com.jpkocommunity.domain.post.service;

import com.jpkocommunity.domain.post.dto.request.PopularPeriod;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.global.config.properties.PopularPostProperties;
import com.jpkocommunity.global.util.KstDateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@RequiredArgsConstructor
public class PopularPostCache {

    private static final int CACHED_SIZE = 50;
    private final PostRepository postRepository;
    private final PopularPostProperties properties;
    private final KstDateUtils kstDateUtils;
    private final Map<PopularPeriod, List<Long>> cache = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${app.popular-post.refresh-interval-minutes}",
            timeUnit = TimeUnit.MINUTES)
    public void refresh() {
        for (PopularPeriod period : PopularPeriod.values()) {
            try {
                cache.put(period, query(period, CACHED_SIZE));
            } catch (Exception e) {
                log.warn("[인기글] 캐시 갱신 실패, period={}", period, e);
            }
        }
    }

    public List<Long> getPopularPostIds(PopularPeriod period, int limit) {
        List<Long> cached = cache.get(period);

        if (cached == null) {
            return query(period, limit);
        }

        return cached.subList(0, Math.min(limit, cached.size()));
    }

    private List<Long> query(PopularPeriod period, int limit) {
        PopularPostProperties.Period config = switch (period) {
            case TODAY -> properties.today();
            case WEEKLY -> properties.weekly();
        };
        LocalDateTime now = kstDateUtils.now();

        // 자정 정렬
        LocalDateTime since = kstDateUtils.startOfToday().minusDays(config.days() - 1L);
        return postRepository.findPopularPostIds(
                since,
                now,
                properties.likeWeight(),
                properties.commentWeight(),
                properties.viewWeight(),
                config.gravity(),
                config.minEngagement(),
                limit
        );
    }
}
