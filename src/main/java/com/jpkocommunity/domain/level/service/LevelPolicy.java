package com.jpkocommunity.domain.level.service;

/**
 * EXP와 레벨 간의 변환 정책을 정의한 클래스
 * EXP_PER_LEVEL만큼 누적 EXP 증가할 때마다 레벨 1씩 증가하게
 */
public final class LevelPolicy {

    public static final long EXP_PER_LEVEL = 500L;
    public static final int MIN_LEVEL = 1;

    private LevelPolicy() {
    }

    // 누적 EXP를 현재 레벨로 변환
    public static int levelOfExp(long exp) {
        return (int) (exp / EXP_PER_LEVEL) + MIN_LEVEL;
    }

    // 현재 레벨 구간에서 모은 누적 exp
    public static long expIntoLevel(long exp) {
        return exp % EXP_PER_LEVEL;
    }
}
