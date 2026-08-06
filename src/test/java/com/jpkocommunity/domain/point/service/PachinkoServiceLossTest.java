package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.PachinkoPlayResponse;
import com.jpkocommunity.domain.point.entity.Point;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// PachinkoServiceFeeTest의 손실 분기 버전. right-probability=0.0으로 고정해 구슬마다 항상 첫 슬롯
// (배율 0.0, 완전 손실)에 착지하는 케이스를 검증한다. @TestPropertySource는 클래스 단위라
// 승/패를 같은 클래스에서 다르게 강제할 수 없어 별도 클래스로 분리했다.
@SpringBootTest
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "jwt.secret=test-only-secret-not-used-by-this-test-0123456789abcdef",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000",
        "cookie.secure=false",
        "cookie.same-site=Lax",
        "cookie.domain=localhost",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",
        "app.pachinko.ball-price=100",
        "app.pachinko.min-balls=1",
        "app.pachinko.max-balls=10",
        "app.pachinko.fee-rate=0.05",
        "app.pachinko.rows=1",
        "app.pachinko.right-probability=0.0", // 항상 왼쪽으로 튕겨 슬롯 0(배율 0.0)에 착지
        "app.pachinko.slot-multipliers[0]=0.0",
        "app.pachinko.slot-multipliers[1]=3.0"
})
class PachinkoServiceLossTest {

    private static final int BALL_COUNT = 3;
    private static final long BALL_PRICE = 100L;
    private static final long TOTAL_COST = BALL_COUNT * BALL_PRICE;
    private static final long EXPECTED_FEE = 15L; // round(300 * 0.05)
    private static final long INITIAL_BALANCE = 10_000L;

    @Autowired
    private PachinkoService pachinkoService;
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

        User user = User.builder()
                .email("pachinko-loss-" + suffix + "@test.local")
                .password("test-password")
                .nickname("pachinko-l-" + suffix)
                .build();
        user.verifyEmail();
        user = userRepository.save(user);

        pointRepository.save(Point.builder().user(user).balance(INITIAL_BALANCE).build());

        userId = user.getId();
    }

    @Test
    void 구슬_여러_개가_모두_배율_0인_슬롯에_착지하면_총_비용_전액과_수수료가_함께_빠진다() {
        PachinkoPlayResponse response = pachinkoService.play(userId, BALL_COUNT);

        assertThat(response.balls()).hasSize(BALL_COUNT);
        response.balls().forEach(ball -> {
            assertThat(ball.bounces()).containsExactly(false);
            assertThat(ball.slotIndex()).isEqualTo(0);
            assertThat(ball.multiplier()).isEqualTo(0.0);
            assertThat(ball.payout()).isZero();
        });
        assertThat(response.fee()).isEqualTo(EXPECTED_FEE);
        assertThat(response.totalPayout()).isZero();
        assertThat(response.netAmount()).isEqualTo(-TOTAL_COST);
        assertThat(response.win()).isFalse();

        Point userPoint = pointRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        assertThat(userPoint.getBalance())
                .as("초기 잔액 - 수수료 - 총 구슬 비용 전액")
                .isEqualTo(INITIAL_BALANCE - EXPECTED_FEE - TOTAL_COST);

        var settlementTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.PACHINKO)
                .filter(t -> t.getSender() != null && t.getSender().getId().equals(userId))
                .toList();
        assertThat(settlementTransactions)
                .as("PACHINKO 정산 거래가 구슬 개수와 무관하게 합산되어 정확히 1건, 유저 -> 시스템 방향으로 남아야 함")
                .hasSize(1);
        assertThat(settlementTransactions.get(0).getAmount()).isEqualTo(TOTAL_COST);
    }
}
