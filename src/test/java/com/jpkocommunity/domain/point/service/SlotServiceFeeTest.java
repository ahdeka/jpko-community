package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.SlotPlayResponse;
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

// 슬롯머신 수수료(SLOT_FEE)와 결과 정산(SLOT)이 별개의 원장 기록으로 남는지 확인하는 회귀 테스트.
// 심볼을 1개만 구성해(다른 후보가 없으므로 어떤 릴을 뽑아도 그 심볼) 트리플 매치를 강제한다.
// (릴 3개가 독립이라 설정만으로 "특정 논매치"를 강제할 수 없어, 손실 케이스는 결정론적 테스트에서 제외.
// 룰렛/파칭코와 달리 이 한계는 다수 심볼을 쓰는 슬롯 설계 자체에서 오는 것.)
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
        "app.slot.bet-unit=100",
        "app.slot.min-bet=100",
        "app.slot.max-bet=100",
        "app.slot.fee-rate=0.05",
        "app.slot.symbols[0].name=SEVEN",
        "app.slot.symbols[0].weight=1",
        "app.slot.symbols[0].multiplier=5.0"
})
class SlotServiceFeeTest {

    private static final long BET_AMOUNT = 100L;
    private static final long EXPECTED_FEE = 5L; // round(100 * 0.05)
    private static final long INITIAL_BALANCE = 1_000L;

    @Autowired
    private SlotService slotService;
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
                .email("slot-user-" + suffix + "@test.local")
                .password("test-password")
                .nickname("slot-" + suffix)
                .build();
        user.verifyEmail(); // 슬롯머신은 이메일 인증된 유저만 이용 가능
        user = userRepository.save(user);

        pointRepository.save(Point.builder().user(user).balance(INITIAL_BALANCE).build());

        userId = user.getId();
    }

    @Test
    void 심볼이_하나뿐이면_항상_트리플_매치로_당첨되고_수수료와_순이익이_각각_정상_기록된다() {
        SlotPlayResponse response = slotService.play(userId, BET_AMOUNT);

        assertThat(response.symbols()).containsExactly("SEVEN", "SEVEN", "SEVEN");
        assertThat(response.tripleMatch()).isTrue();
        assertThat(response.multiplier()).isEqualTo(5.0);
        assertThat(response.fee()).isEqualTo(EXPECTED_FEE);
        assertThat(response.payout()).isEqualTo(BET_AMOUNT * 5);
        assertThat(response.netAmount()).isEqualTo(BET_AMOUNT * 4);
        assertThat(response.win()).isTrue();

        Point userPoint = pointRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        assertThat(userPoint.getBalance())
                .as("초기 잔액 - 수수료 + 순이익")
                .isEqualTo(INITIAL_BALANCE - EXPECTED_FEE + BET_AMOUNT * 4);

        var feeTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.SLOT_FEE)
                .filter(t -> t.getSender() != null && t.getSender().getId().equals(userId))
                .toList();
        assertThat(feeTransactions).as("SLOT_FEE 거래 기록이 정확히 1건 남아야 함").hasSize(1);
        assertThat(feeTransactions.get(0).getAmount()).isEqualTo(EXPECTED_FEE);

        var settlementTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.SLOT)
                .filter(t -> t.getRecipient() != null && t.getRecipient().getId().equals(userId))
                .toList();
        assertThat(settlementTransactions)
                .as("SLOT 정산 거래가 정확히 1건 남아야 함(수수료와는 별개 기록)")
                .hasSize(1);
        assertThat(settlementTransactions.get(0).getAmount()).isEqualTo(BET_AMOUNT * 4);
    }
}
