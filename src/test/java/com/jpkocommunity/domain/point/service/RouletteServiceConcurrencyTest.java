package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.RouletteOutcome;
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

// 로그인 시 룰렛의 핵심 동시성 보장(같은 당첨자가 동시에 두 번 지급받지 않고,
// 몰수했다가 지급이 무산된 만큼은 반드시 피해자에게 환불됨)을 실제 Postgres 락으로 검증한다.
//
// 로컬 실행 방법:
//   DB_USER=jpko DB_PASSWORD=jpko docker compose up -d
//   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/jpko_community \
//   SPRING_DATASOURCE_USERNAME=jpko SPRING_DATASOURCE_PASSWORD=jpko \
//   ./gradlew test --tests RouletteServiceConcurrencyTest
@SpringBootTest
@TestPropertySource(properties = {
        // "dev" 프로필 비활성화 -> DevDataInitializer(데모 유저 10명 시드)가 실행되지 않도록 함.
        // 시드 유저가 있으면 findRandomActiveUserIdExcluding()이 테스트가 통제하지 않는 유저를
        // 피해자로 뽑을 수 있어 아래 결정론적 검증(고정 금액)이 흔들린다.
        "spring.profiles.active=test",
        "jwt.secret=test-only-secret-not-used-by-this-test-0123456789abcdef",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000",
        "cookie.secure=false",
        "cookie.same-site=Lax",
        "cookie.domain=localhost",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",
        // 결과를 결정론적으로 검증하기 위해 룰렛 금액을 고정값으로 둔다
        "app.roulette.min-amount=100",
        "app.roulette.max-amount=100"
})
class RouletteServiceConcurrencyTest {

    private static final long FIXED_ROULETTE_AMOUNT = 100L;
    private static final long VICTIM_INITIAL_BALANCE = 10_000L;

    @Autowired
    private RouletteService rouletteService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private Long winnerId;
    private Long victimId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User winner = userRepository.save(User.builder()
                .email("roulette-winner-" + suffix + "@test.local")
                .password("test-password")
                .nickname("winner-" + suffix)
                .build());
        User victim = userRepository.save(User.builder()
                .email("roulette-victim-" + suffix + "@test.local")
                .password("test-password")
                .nickname("victim-" + suffix)
                .build());

        pointRepository.save(Point.builder().user(winner).balance(0L).build());
        pointRepository.save(Point.builder().user(victim).balance(VICTIM_INITIAL_BALANCE).build());

        winnerId = winner.getId();
        victimId = victim.getId();
    }

    @Test
    void 동시에_두번_스핀해도_당첨자는_한번만_지급받고_나머지는_피해자에게_환불된다() throws Exception {
        long systemBalanceBefore = pointRepository.findByUserIsNull().orElseThrow().getBalance();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<RouletteOutcome> results = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = IntStream.range(0, threadCount)
                .<Future<?>>mapToObj(i -> executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    // 오직 하나의 후보(victim)만 존재하므로 두 스레드 모두 같은 유저를 대상으로 스핀한다
                    results.add(rouletteService.spinIfEligible(winnerId));
                }))
                .toList();

        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(results).hasSize(2);
        long spunCount = results.stream().filter(RouletteOutcome::spun).count();
        assertThat(spunCount)
                .as("동시에 두 번 스핀해도 딱 한 번만 지급되어야 함")
                .isEqualTo(1);

        Point winnerPoint = pointRepository.findByUser(userRepository.findById(winnerId).orElseThrow()).orElseThrow();
        assertThat(winnerPoint.getBalance())
                .as("당첨자는 정확히 한 번만 지급받아야 함 (중복 지급 시 200이 됨)")
                .isEqualTo(FIXED_ROULETTE_AMOUNT);

        Point victimPoint = pointRepository.findByUser(userRepository.findById(victimId).orElseThrow()).orElseThrow();
        assertThat(victimPoint.getBalance())
                .as("피해자는 정확히 한 번만 털려야 함 (몰수+환불이 상쇄되지 않으면 9800이 됨)")
                .isEqualTo(VICTIM_INITIAL_BALANCE - FIXED_ROULETTE_AMOUNT);

        long systemBalanceAfter = pointRepository.findByUserIsNull().orElseThrow().getBalance();
        assertThat(systemBalanceAfter)
                .as("포인트는 항상 시스템 계정을 거치므로, 스핀이 끝나면 시스템 잔액은 원래대로 돌아와야 함")
                .isEqualTo(systemBalanceBefore);

        List<Transaction> winnerGrants = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.ROULETTE)
                .filter(t -> t.getRecipient() != null && t.getRecipient().getId().equals(winnerId))
                .toList();
        assertThat(winnerGrants)
                .as("당첨자에게 지급된 ROULETTE 거래 기록도 정확히 1건이어야 함")
                .hasSize(1);

        List<Transaction> refunds = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.GRANT)
                .filter(t -> t.getRecipient() != null && t.getRecipient().getId().equals(victimId))
                .filter(t -> t.getMessage() != null && t.getMessage().contains("환불"))
                .toList();
        assertThat(refunds)
                .as("경쟁에서 진 스핀은 피해자에게 환불 트랜잭션을 남겨야 함")
                .hasSize(1);
    }
}
