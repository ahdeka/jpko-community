package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.repository.UserRepository;
import com.jpkocommunity.domain.point.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 원장(Transaction)은 항상 양수 금액 + sender/recipient로 방향을 나타내는 관례를 쓰는데,
// CheckInOutcome은 로그인/회원가입 응답에서 "얻었는지 잃었는지"를 amount의 부호로 표현해야 한다.
// 이 둘을 잘못 이어붙이면 마이너스 결과도 항상 "획득"으로 보이는 버그가 생길 수 있어 회귀 테스트로 고정한다.
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
        // 항상 -200으로만 나오도록 구간을 1개(음수)만 둔다
        "app.checkin.unit=50",
        "app.checkin.ranges[0].min-amount=-200",
        "app.checkin.ranges[0].max-amount=-200",
        "app.checkin.ranges[0].weight=1",
        "app.roulette.bet-unit=100",
        "app.roulette.min-bet=100",
        "app.roulette.max-bet=100",
        "app.roulette.win-probability=0.5",
        "app.roulette.fee-rate=0.05"
})
class CheckInServiceSignTest {

    private static final long INITIAL_BALANCE = 1_000L;
    private static final long FIXED_LOSS = 200L;

    @Autowired
    private CheckInService checkInService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .email("checkin-sign-" + suffix + "@test.local")
                .password("test-password")
                .nickname("checkin-sign-" + suffix)
                .build());
        pointRepository.save(Point.builder().user(user).balance(INITIAL_BALANCE).build());
        userId = user.getId();
    }

    @Test
    void 마이너스_출첵_결과는_음수_amount로_내려온다() {
        CheckInOutcome outcome = checkInService.checkInIfEligible(userId);

        assertThat(outcome.checkedIn()).isTrue();
        assertThat(outcome.amount())
                .as("손실 구간만 설정했으므로 amount는 음수(-200)여야 함 - 양수로 오면 '획득'으로 오인되는 버그")
                .isEqualTo(-FIXED_LOSS);

        Point point = pointRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        assertThat(point.getBalance()).isEqualTo(INITIAL_BALANCE - FIXED_LOSS);
    }
}
