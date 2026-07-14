package com.jpkocommunity.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGrade {
    ASHIGARU("일반 회원"),
    SAMURAI("우수 회원"),
    HATAMOTO("특별 회원"),
    DAIMYO("대표 회원"),
    SHOGUN("운영진");

    private final String displayGradeName;

}