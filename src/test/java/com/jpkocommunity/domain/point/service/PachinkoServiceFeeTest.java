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

// 파칭코 수수료(PACHINKO_FEE)와 결과 정산(PACHINKO)이 별개의 원장 기록으로 남는지, 그리고 구슬
// 여러 개를 쐈을 때 결과가 정확히 합산되는지 확인하는 회귀 테스트.
// right-probability=1.0으로 고정해 구슬마다 항상 마지막 슬롯(배율 3.0)에 착지하는 승리 케이스를 검증한다.
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
        "app.pachinko.right-probability=1.0", // 항상 오른쪽으로 튕겨 슬롯 1(배율 3.0)에 착지
        "app.pachinko.slot-multipliers[0]=0.0",
        "app.pachinko.slot-multipliers[1]=3.0"
})
class PachinkoServiceFeeTest {

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
                .email("pachinko-user-" + suffix + "@test.local")
                .password("test-password")
                .nickname("pachinko-" + suffix)
                .build();
        user.verifyEmail(); // 파칭코는 이메일 인증된 유저만 이용 가능
        user = userRepository.save(user);

        pointRepository.save(Point.builder().user(user).balance(INITIAL_BALANCE).build());

        userId = user.getId();
    }

    @Test
    void 구슬_여러_개가_모두_배율_3배_슬롯에_착지하면_합산된_수수료와_순이익이_정상_기록된다() {
        PachinkoPlayResponse response = pachinkoService.play(userId, BALL_COUNT);

        assertThat(response.ballCount()).isEqualTo(BALL_COUNT);
        assertThat(response.ballPrice()).isEqualTo(BALL_PRICE);
        assertThat(response.totalCost()).isEqualTo(TOTAL_COST);
        assertThat(response.fee()).isEqualTo(EXPECTED_FEE);
        assertThat(response.balls()).as("쏜 구슬 개수만큼 결과가 남아야 함").hasSize(BALL_COUNT);
        response.balls().forEach(ball -> {
            assertThat(ball.bounces()).as("rows=1이므로 튕김 기록은 1개, 항상 오른쪽").containsExactly(true);
            assertThat(ball.slotIndex()).isEqualTo(1);
            assertThat(ball.multiplier()).isEqualTo(3.0);
            assertThat(ball.payout()).isEqualTo(BALL_PRICE * 3);
        });
        assertThat(response.totalPayout()).isEqualTo(TOTAL_COST * 3);
        assertThat(response.netAmount()).isEqualTo(TOTAL_COST * 2);
        assertThat(response.win()).isTrue();

        Point userPoint = pointRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        assertThat(userPoint.getBalance())
                .as("초기 잔액 - 수수료 + 순이익")
                .isEqualTo(INITIAL_BALANCE - EXPECTED_FEE + TOTAL_COST * 2);

        var feeTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.PACHINKO_FEE)
                .filter(t -> t.getSender() != null && t.getSender().getId().equals(userId))
                .toList();
        assertThat(feeTransactions).as("PACHINKO_FEE 거래 기록이 정확히 1건 남아야 함").hasSize(1);
        assertThat(feeTransactions.get(0).getAmount()).isEqualTo(EXPECTED_FEE);

        var settlementTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.PACHINKO)
                .filter(t -> t.getRecipient() != null && t.getRecipient().getId().equals(userId))
                .toList();
        assertThat(settlementTransactions)
                .as("PACHINKO 정산 거래가 구슬 개수와 무관하게 합산되어 정확히 1건 남아야 함(수수료와는 별개 기록)")
                .hasSize(1);
        assertThat(settlementTransactions.get(0).getAmount()).isEqualTo(TOTAL_COST * 2);
    }
}
