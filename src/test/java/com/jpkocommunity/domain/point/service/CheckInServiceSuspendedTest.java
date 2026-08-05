package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
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

// 정지(SUSPENDED)된 계정은 출첵 대상에서 제외돼야 한다. 예전에는 PointService.lockAccounts()의
// validateActive가 던진 예외를 CheckInService가 catch해서 흡수했는데, 그 예외가 별도 @Transactional
// 프록시 메서드에서 나온 것이라 이 메서드의 REQUIRES_NEW 트랜잭션이 이미 rollback-only로 마킹되어,
// 커밋 시점에 UnexpectedRollbackException이 터져 로그인 자체가 500으로 실패했다. lockAccounts를
// 아예 타지 않도록 사전에 상태를 확인하는지 검증한다.
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
class CheckInServiceSuspendedTest {

    private static final long INITIAL_BALANCE = 1_000L;

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
        User user = User.builder()
                .email("checkin-suspended-" + suffix + "@test.local")
                .password("test-password")
                .nickname("checkin-suspended-" + suffix)
                .build();
        user.suspend(null); // ROOT의 무기한 정지와 동일한 상태
        user = userRepository.save(user);
        pointRepository.save(Point.builder().user(user).balance(INITIAL_BALANCE).build());
        userId = user.getId();
    }

    @Test
    void 정지된_계정은_예외_없이_출첵이_스킵된다() {
        CheckInOutcome outcome = checkInService.checkInIfEligible(userId);

        assertThat(outcome.checkedIn()).as("정지 계정은 출첵 대상이 아니므로 checkedIn=false여야 함").isFalse();

        Point point = pointRepository.findByUser(userRepository.findById(userId).orElseThrow()).orElseThrow();
        assertThat(point.getBalance()).as("포인트가 변하지 않아야 함").isEqualTo(INITIAL_BALANCE);

        assertThat(transactionRepository.findAll().stream()
                .anyMatch(t -> t.getRecipient() != null && t.getRecipient().getId().equals(userId)
                        || t.getSender() != null && t.getSender().getId().equals(userId)))
                .as("출첵 관련 원장 기록이 남지 않아야 함")
                .isFalse();
    }
}
