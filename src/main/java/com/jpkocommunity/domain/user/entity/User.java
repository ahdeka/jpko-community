package com.jpkocommunity.domain.user.entity;

import com.jpkocommunity.global.entity.BaseEntity;
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

    @Column(nullable = false)
    private boolean emailVerified;

    @Builder
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.grade = UserGrade.ASHIGARU;
        this.emailVerified = false;
        LocalDateTime now = LocalDateTime.now();
        this.termsAgreedAt = now;
        this.privacyAgreedAt = now;
    }

    // ========== 비즈니스 메서드 ==========

    public void updateNickname(String nickname) {
        this.nickname = nickname;
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

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void updateBio(String bio) {
        this.bio = bio;
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
