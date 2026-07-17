package com.jpkocommunity.global.config;

/**
 * pg_advisory_xact_lock(classifier, userId) 호출 시 쓰는 namespace 상수 생성
 *
 * advisory lock은 숫자 키가 같으면 무조건 같은 락으로 취급해서
 * 게시글 작성과 댓글 작성에 대한 무관한 행위가 같은 유저 락을 공유하니까
 * 이걸 분류하기 위해 classifier값으로 분류
 */
public final class ThrottleLockClassifier {
    private ThrottleLockClassifier() {}
    public static final int POST = 1;
    public static final int COMMENT = 2;
}
