package com.jpkocommunity.domain.user.entity;

import com.jpkocommunity.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

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

    // 이용약관
    @Column(nullable = false)
    private LocalDateTime termsAgreedAt;

    // 개인정보처리방침
    @Column(nullable = false)
    private LocalDateTime privacyAgreedAt;

    @Builder
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
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
}
