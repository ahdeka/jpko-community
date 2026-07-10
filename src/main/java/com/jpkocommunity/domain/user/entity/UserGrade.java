package com.jpkocommunity.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGrade {
    ASHIGARU("아시가루"),
    SAMURAI("사무라이"),
    HATAMOTO("하타모토"),
    DAIMYO("다이묘"),       // 최상위 우수회원
    SHOGUN("쇼군");         // 운영진 전용

    private final String displayGradeName;
}