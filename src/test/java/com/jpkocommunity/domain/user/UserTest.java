package com.jpkocommunity.domain.user;

import com.jpkocommunity.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {
    @Test
    void 변경이력_없으면_변경_가능() {
        User user = User.builder().nickname("초기닉네임").build();
        assertThat(user.isNicknameChangeable(LocalDateTime.now())).isTrue();
    }
    @Test
    void 쿨타임_중에는_변경_불가() {
        User user = User.builder().nickname("초기닉네임").build();
        user.updateNickname("새닉네임");

        // 변경 시점으로부터 29일 뒤를 "현재"로 가정
        assertThat(user.isNicknameChangeable(LocalDateTime.now().plusDays(29))).isFalse();
        assertThat(user.isNicknameChangeable(LocalDateTime.now().plusDays(31))).isTrue();
    }
}
