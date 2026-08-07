package com.jpkocommunity.domain.user.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 20)
    private String loginId;

    @Column
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserGrade grade;

    @Column(length = 200)
    private String bio;

    // 이용약관
    @Column(nullable = false)
    private LocalDateTime termsAgreedAt;

    // 개인정보처리방침
    @Column(nullable = false)
    private LocalDateTime privacyAgreedAt;

    @Column
    private LocalDateTime deletedAt;

    // 정지 기한. NULL이면 무기한 정지(기존 ADMIN/ROOT 기본 동작).
    // 경매로 획득한 ADMIN 권한이 정지시킬 때는 반드시 값이 있어야 한다.
    @Column
    private LocalDateTime suspendedUntil;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean rouletteStatus;

    @Builder
    public User(String email, String password, String nickname, String loginId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.loginId = loginId;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.grade = UserGrade.ASHIGARU;
        this.emailVerified = false;
        LocalDateTime now = LocalDateTime.now();
        this.termsAgreedAt = now;
        this.privacyAgreedAt = now;
        this.rouletteStatus = false;
    }

    // ========== 비즈니스 메서드 ==========

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // TODO 26-feature 마이그레이션 끝나면 정리될 구문
    public void assignLoginId(String loginId) {
        if (this.loginId != null) {
            throw new CustomException(ErrorCode.ALREADY_ASSIGN_LOGIN_ID);
        }
        this.loginId = loginId;
    }

    // TODO 26-feature 마이그레이션 끝나면 정리될 구문
    public boolean isFullyOnboarded() {
        return this.loginId != null && this.emailVerified;
    }

    // email 교체 + 인증 완료 TODO 마이그레이션 끝나면 정리
    public void changeEmail(String newEmail) {
        this.email = newEmail;
        this.emailVerified = true;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updateGrade(UserGrade grade) {
        this.grade = grade;
    }

    public void suspend(LocalDateTime suspendedUntil) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = suspendedUntil;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void agreeToRoulette() {
        this.rouletteStatus = true;
    }

    public void cancelRouletteAgreement() {
        this.rouletteStatus = false;
    }

    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.nickname = "탈퇴회원#" + getId();
        this.email = "deleted_" + getId() + "_" + java.util.UUID.randomUUID() + "@jpkocommunity.local";
    }

    public String getDisplayNickname() {
        return isDeleted() ? "(탈퇴한 회원)" : this.nickname;
    }

    public boolean isDeleted() {
        return this.status == UserStatus.DELETED;
    }
}
