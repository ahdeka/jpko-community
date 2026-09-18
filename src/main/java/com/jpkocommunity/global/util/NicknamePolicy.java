package com.jpkocommunity.global.util;

import java.util.Locale;
import java.util.Set;


/**
 * 닉네임 금칙어 정책
 */
public final class NicknamePolicy {

    private NicknamePolicy() {
    }

    private static final Set<String> EXACT_BLACKLIST = Set.of(
            // 운영진 사칭목록
            "관리자", "운영자", "운영팀", "매니저", "고객센터",
            "admin", "administrator", "root", "system",

            // 사이트에 관련된 문구
            "탈퇴회원", "탈퇴한회원",
            "jpko", "jpkocommunity",

            // 비속어
            "보지", "자지"
    );

    private static final Set<String> CONTAINS_BLACKLIST = Set.of(
            "씨발", "병신", "섹스", "sex", "fuck"
    );

    public static boolean isBlackList(String nickname) {
        if (nickname == null) return false;

        String normalized = nickname.toLowerCase(Locale.ROOT);

        if (EXACT_BLACKLIST.contains(normalized)) return true;

        return CONTAINS_BLACKLIST.stream().anyMatch(normalized::contains);
    }
}
