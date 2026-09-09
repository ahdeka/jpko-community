package com.jpkocommunity.domain.level.entity;

/**
 * 경험치를 받을 수 있는 행위들
 */
public enum ExpAction {
    CREATE_POST(true),
    CREATE_COMMENT(true),
    CHECK_IN(false);
    private final boolean dailyLimited;

    ExpAction(boolean dailyLimited) {
        this.dailyLimited = dailyLimited;
    }

    public boolean isDailyLimited() {
        return dailyLimited;
    }
}
