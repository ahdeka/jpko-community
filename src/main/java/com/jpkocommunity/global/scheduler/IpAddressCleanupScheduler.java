package com.jpkocommunity.global.scheduler;

import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 개인정보처리방침 "보유 및 이용 기간" 조항 이행
 * IP 기록 데이터 특정 기간 보관 후 파기 용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpAddressCleanupScheduler {

    // IP 기록 데이터 보관 기간 (6개월)
    private static final int IP_RETENTION_MONTHS = 6;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 매일 새벽 4시 5분에 실행
    @Scheduled(cron = "0 5 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void anonymizeOldIpAddresses() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(IP_RETENTION_MONTHS);

        int postCount = postRepository.anonymizeIpBefore(cutoff);
        int commentCount = commentRepository.anonymizeIpBefore(cutoff);

        log.info("[IP 파기] {}개월 경과 게시글 {}건, 댓글 {}건 IP 삭제 완료",
                IP_RETENTION_MONTHS, postCount, commentCount);
    }
}
