package com.jpkocommunity.global.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class NicknamePolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"관리자", "운영자", "admin", "AdMiN", "탈퇴회원", "보지"})
    void 완전일치_금칙어는_차단된다(String nickname) {
        assertThat(NicknamePolicy.isBlackList(nickname)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"존나섹스맨", "씨발놈", "IloveSEX", "병신같은놈"})
    void 부분일치_금칙어는_차단된다(String nickname) {
        assertThat(NicknamePolicy.isBlackList(nickname)).isTrue();
    }
    @ParameterizedTest
    @ValueSource(strings = {"나는관리자", "해보지마", "나혼자지구", "김철수", "adminis"})
    void 오탐이_발생하지_않는다(String nickname) {
        assertThat(NicknamePolicy.isBlackList(nickname)).isFalse();
    }

    @Test
    void null은_차단하지_않는다() {
        assertThat(NicknamePolicy.isBlackList(null)).isFalse();
    }
}
