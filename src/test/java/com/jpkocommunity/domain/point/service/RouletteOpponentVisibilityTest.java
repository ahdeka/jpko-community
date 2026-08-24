package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.RouletteOpponentResponse;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.domain.point.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// getOpponent()가 spin()과 같은 기준(isEligibleOpponent)으로 필터링하는지 확인하는 회귀 테스트.
// 이 필터가 없으면 상대가 정지돼도 화면엔 계속 "고정된 상대"로 보이는데, 실제 다음 스핀에서는
// 조용히 랜덤 상대로 대체돼 표시와 실제 동작이 어긋난다.
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
        "app.roulette.win-probability=0.5",
        "app.roulette.fee-rate=0.05"
})
@Disabled("미검증된 코드 - 마이그레이션 및 리팩톨이 작업 시 재검토 예정 2026.08.24")
class RouletteOpponentVisibilityTest {

    @Autowired
    private RouletteService rouletteService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;

    private Long userId;
    private Long opponentId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .email("roulette-visibility-user-" + suffix + "@test.local")
                .password("test-password")
                .nickname("visibility-user-" + suffix)
                .build();
        user.verifyEmail();
        user = userRepository.save(user);
        pointRepository.save(Point.builder().user(user).balance(1_000L).build());

        User opponent = User.builder()
                .email("roulette-visibility-opponent-" + suffix + "@test.local")
                .password("test-password")
                .nickname("visibility-opponent-" + suffix)
                .build();
        opponent.verifyEmail();
        opponent = userRepository.save(opponent);
        pointRepository.save(Point.builder().user(opponent).balance(0L).build());

        userId = user.getId();
        opponentId = opponent.getId();

        rouletteService.lockOpponent(userId, opponentId, null);
    }

    @Test
    void 고정된_상대가_정지되면_조회에서_제외된다() {
        assertThat(rouletteService.getOpponent(userId, null))
                .as("정지 전에는 정상적으로 고정된 상대가 조회돼야 함")
                .map(RouletteOpponentResponse::opponentId)
                .contains(opponentId);

        User opponent = userRepository.findById(opponentId).orElseThrow();
        opponent.suspend(null);
        userRepository.save(opponent);

        Optional<RouletteOpponentResponse> afterSuspend = rouletteService.getOpponent(userId, null);
        assertThat(afterSuspend)
                .as("상대가 정지되면 다음 스핀에서 실제로 쓰이지 않을 것이므로 조회에서도 빠져야 함")
                .isEmpty();
    }
}
