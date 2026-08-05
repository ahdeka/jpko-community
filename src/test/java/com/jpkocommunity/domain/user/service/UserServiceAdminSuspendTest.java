package com.jpkocommunity.domain.user.service;

import com.jpkocommunity.domain.shop.entity.ActivityBlock;
import com.jpkocommunity.domain.shop.entity.BlockStatus;
import com.jpkocommunity.domain.shop.repository.ActivityBlockRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;
import com.jpkocommunity.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// ADMIN(진짜 ROOT가 아닌)이 관리자 페이지에서 정지를 걸면 User.status는 건드리지 않고
// ActivityBlock으로 완전히 대체돼야 한다 - 그래야 BlockAppeal로 ROOT가 감시할 수 있고, 대상도
// 상점 차단과 동일하게 자가해제할 수 있다. ROOT의 정지는 여전히 User.status=SUSPENDED로 남아야
// 한다("진짜 관리자는 ROOT뿐"이라는 원칙).
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
class UserServiceAdminSuspendTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActivityBlockRepository activityBlockRepository;

    private Long adminId;
    private Long rootId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User admin = User.builder()
                .email("admin-suspend-" + suffix + "@test.local")
                .password("test-password")
                .nickname("admin-suspend-" + suffix)
                .build();
        admin.updateRole(UserRole.ADMIN);
        adminId = userRepository.save(admin).getId();

        User root = User.builder()
                .email("root-suspend-" + suffix + "@test.local")
                .password("test-password")
                .nickname("root-suspend-" + suffix)
                .build();
        root.updateRole(UserRole.ROOT);
        rootId = userRepository.save(root).getId();
    }

    private Long createTargetUser(String suffix) {
        User target = User.builder()
                .email("target-" + suffix + "@test.local")
                .password("test-password")
                .nickname("target-" + suffix)
                .build();
        return userRepository.save(target).getId();
    }

    @Test
    void ADMIN이_정지시키면_User_status는_그대로이고_ActivityBlock으로_대체된다() {
        Long targetId = createTargetUser(UUID.randomUUID().toString().substring(0, 8));
        User target = userRepository.findById(targetId).orElseThrow();
        LocalDateTime suspendedUntil = LocalDateTime.now().plusDays(1);

        userService.updateStatus(targetId, UserStatus.SUSPENDED, suspendedUntil, adminId);

        User reloaded = userRepository.findById(targetId).orElseThrow();
        assertThat(reloaded.getStatus()).as("ADMIN 정지는 User.status를 건드리면 안 됨").isEqualTo(UserStatus.ACTIVE);

        ActivityBlock block = activityBlockRepository.findByTargetAndStatus(target, BlockStatus.ACTIVE).orElseThrow();
        assertThat(block.getItem()).as("구매한 차단이 아니므로 item은 null이어야 함").isNull();
        assertThat(block.getPrice()).as("SuspendUnlockSetting 기본 시드 가격(1000)을 그대로 써야 함").isEqualTo(1000L);
        assertThat(block.getExpiresAt()).isEqualTo(suspendedUntil);
    }

    @Test
    void ROOT가_정지시키면_기존처럼_User_status가_SUSPENDED로_바뀐다() {
        Long targetId = createTargetUser(UUID.randomUUID().toString().substring(0, 8));
        User target = userRepository.findById(targetId).orElseThrow();

        userService.updateStatus(targetId, UserStatus.SUSPENDED, null, rootId);

        User reloaded = userRepository.findById(targetId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(activityBlockRepository.findByTargetAndStatus(target, BlockStatus.ACTIVE))
                .as("ROOT의 정지는 ActivityBlock을 만들지 않아야 함")
                .isEmpty();
    }

    @Test
    void ADMIN이_해제하면_관리자_정지로_생긴_ActivityBlock이_풀린다() {
        Long targetId = createTargetUser(UUID.randomUUID().toString().substring(0, 8));
        User target = userRepository.findById(targetId).orElseThrow();
        userService.updateStatus(targetId, UserStatus.SUSPENDED, LocalDateTime.now().plusDays(1), adminId);

        userService.updateStatus(targetId, UserStatus.ACTIVE, null, adminId);

        assertThat(activityBlockRepository.findByTargetAndStatus(target, BlockStatus.ACTIVE))
                .as("해제 후에는 활성 차단이 없어야 함")
                .isEmpty();
    }
}
