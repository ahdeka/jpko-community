package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.entity.Transaction;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.PointRepository;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// 출첵의 핵심 동시성 보장(같은 유저가 동시 로그인 등으로 두 번 스핀해도 하루에 한 번만 지급됨)을
// 실제 Postgres 락으로 검증한다.
//
// 로컬 실행 방법:
//   DB_USER=jpko DB_PASSWORD=jpko docker compose up -d
//   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/jpko_community \
//   SPRING_DATASOURCE_USERNAME=jpko SPRING_DATASOURCE_PASSWORD=jpko \
//   ./gradlew test --tests CheckInServiceConcurrencyTest
@SpringBootTest
@TestPropertySource(properties = {
        // "dev" 프로필 비활성화 -> DevDataInitializer(데모 유저 10명 시드)가 실행되지 않도록 함.
        "spring.profiles.active=test",
        "jwt.secret=test-only-secret-not-used-by-this-test-0123456789abcdef",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000",
        "cookie.secure=false",
        "cookie.same-site=Lax",
        "cookie.domain=localhost",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",
        // 결과를 결정론적으로 검증하기 위해 출첵 금액을 구간 1개짜리 고정값으로 둔다
        "app.checkin.unit=100",
        "app.checkin.ranges[0].min-amount=100",
        "app.checkin.ranges[0].max-amount=100",
        "app.checkin.ranges[0].weight=1"
})
class CheckInServiceConcurrencyTest {

    private static final long FIXED_CHECKIN_AMOUNT = 100L;

    @Autowired
    private CheckInService checkInService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = userRepository.save(User.builder()
                .email("checkin-user-" + suffix + "@test.local")
                .password("test-password")
                .nickname("checkin-" + suffix)
                .build());

        pointRepository.save(Point.builder().user(user).balance(0L).build());

        userId = user.getId();
    }

    @Test
    void 동시에_두번_출첵해도_하루에_한번만_지급된다() throws Exception {
        long systemBalanceBefore = pointRepository.findByUserIsNull().orElseThrow().getBalance();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<CheckInOutcome> results = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = IntStream.range(0, threadCount)
                .<Future<?>>mapToObj(i -> executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    results.add(checkInService.checkInIfEligible(userId));
                }))
                .toList();

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(results).hasSize(2);
        long checkedInCount = results.stream().filter(CheckInOutcome::checkedIn).count();
        assertThat(checkedInCount)
                .as("동시에 두 번 출첵해도 딱 한 번만 지급되어야 함")
                .isEqualTo(1);

        Point userPoint = pointRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        assertThat(userPoint.getBalance())
                .as("유저는 정확히 한 번만 지급받아야 함 (중복 지급 시 200이 됨)")
                .isEqualTo(FIXED_CHECKIN_AMOUNT);

        long systemBalanceAfter = pointRepository.findByUserIsNull().orElseThrow().getBalance();
        assertThat(systemBalanceAfter)
                .as("시스템 계정은 지급한 만큼만 감소해야 함(이중 지급되지 않음)")
                .isEqualTo(systemBalanceBefore - FIXED_CHECKIN_AMOUNT);

        List<Transaction> checkins = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.CHECKIN)
                .filter(t -> t.getRecipient() != null && t.getRecipient().getId().equals(userId))
                .toList();
        assertThat(checkins)
                .as("유저에게 지급된 CHECKIN 거래 기록도 정확히 1건이어야 함")
                .hasSize(1);
    }
}
