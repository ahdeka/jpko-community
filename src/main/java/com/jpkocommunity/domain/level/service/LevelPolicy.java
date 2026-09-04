package com.jpkocommunity.domain.level.service;

/**
 * EXP와 레벨 간의 변환 정책을 정의한 클래스
 * EXP_PER_LEVEL만큼 누적 EXP 증가할 때마다 레벨 1씩 증가하게
 */
public final class LevelPolicy {

    public static final long EXP_PER_LEVEL = 500L;

    private LevelPolicy() {
    }

    // 누적 EXP를 현재 레벨로 변환
    public static int levelOfExp(long exp) {
        if (exp <= 0) return 0;
        return (int) (exp / EXP_PER_LEVEL);
    }

    // 해당 레벨에 도달하기 위해 필요한 누적 exp
    public static long expForLevel(int level) {
        return level <= 0 ? 0L : EXP_PER_LEVEL * level;
    }

    // 현재 레벨 구간에서 모은 누적 exp
    public static long expIntoLevel(long exp) {
        return exp <= 0 ? 0L : exp % EXP_PER_LEVEL;
    }
}
