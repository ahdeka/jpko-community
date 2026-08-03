package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.RouletteSpinResponse;
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

// "상대 잔액이 0이라 0포인트로 승리해도 수수료는 정상적으로 차감되는가"를 확인하는 회귀 테스트.
// 사용자가 "0포인트 승리 시 수수료 차감이 안 됨"을 의심 보고해 정확히 이 시나리오를 고정값으로 재현한다.
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
        "app.checkin.unit=100",
        "app.checkin.ranges[0].min-amount=100",
        "app.checkin.ranges[0].max-amount=100",
        "app.checkin.ranges[0].weight=1",
        "app.roulette.bet-unit=100",
        "app.roulette.min-bet=100",
        "app.roulette.max-bet=100",
        "app.roulette.win-probability=1.0", // initiator가 항상 승리하도록 고정
        "app.roulette.fee-rate=0.05"
})
class RouletteServiceFeeTest {

    private static final long BET_AMOUNT = 100L;
    private static final long EXPECTED_FEE = 5L; // round(100 * 0.05)
    private static final long INITIATOR_INITIAL_BALANCE = 1_000L;

    @Autowired
    private RouletteService rouletteService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private Long initiatorId;
    private Long opponentId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User initiator = userRepository.save(User.builder()
                .email("roulette-initiator-" + suffix + "@test.local")
                .password("test-password")
                .nickname("initiator-" + suffix)
                .build());
        User opponent = userRepository.save(User.builder()
                .email("roulette-opponent-" + suffix + "@test.local")
                .password("test-password")
                .nickname("opponent-" + suffix)
                .build());

        pointRepository.save(Point.builder().user(initiator).balance(INITIATOR_INITIAL_BALANCE).build());
        pointRepository.save(Point.builder().user(opponent).balance(0L).build());

        initiatorId = initiator.getId();
        opponentId = opponent.getId();

        // 지속되는(persistent) 테스트 DB에는 다른 테스트가 남긴 유저도 섞여 있어 랜덤 상대 선정에
        // 기댈 수 없다 -> 상대를 명시적으로 고정해 이 테스트가 항상 opponent(잔액 0)를 상대하게 한다.
        rouletteService.lockOpponent(initiatorId, opponentId, BET_AMOUNT + EXPECTED_FEE);
    }

    @Test
    void 상대_잔액이_0이라_0포인트로_승리해도_수수료는_차감된다() {
        RouletteSpinResponse response = rouletteService.spin(initiatorId, BET_AMOUNT);

        assertThat(response.win()).as("승률 1.0으로 고정했으므로 반드시 승리해야 함").isTrue();
        assertThat(response.resultAmount()).as("상대 잔액이 0이므로 실제로 오간 금액은 0이어야 함").isZero();
        assertThat(response.fee()).as("수수료는 배팅액 기준으로 계산되어 resultAmount와 무관하게 5여야 함").isEqualTo(EXPECTED_FEE);

        Point initiatorPoint = pointRepository.findByUser(userRepository.findById(initiatorId).orElseThrow()).orElseThrow();
        assertThat(initiatorPoint.getBalance())
                .as("승리로 얻은 포인트는 0이지만 수수료 5는 실제로 잔액에서 빠져야 함")
                .isEqualTo(INITIATOR_INITIAL_BALANCE - EXPECTED_FEE);

        var feeTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.ROULETTE_FEE)
                .filter(t -> t.getSender() != null && t.getSender().getId().equals(initiatorId))
                .toList();
        assertThat(feeTransactions)
                .as("ROULETTE_FEE 거래 기록이 정확히 1건 남아야 함")
                .hasSize(1);
        assertThat(feeTransactions.get(0).getAmount()).isEqualTo(EXPECTED_FEE);

        var duelTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.ROULETTE)
                .filter(t -> t.getRecipient() != null && t.getRecipient().getId().equals(initiatorId))
                .toList();
        assertThat(duelTransactions)
                .as("ROULETTE 정산 거래도 amount=0으로 1건 남아야 함(감사 로그 목적)")
                .hasSize(1);
        assertThat(duelTransactions.get(0).getAmount()).isZero();
    }
}
