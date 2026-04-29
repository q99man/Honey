package com.honeytong.user.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "language_preference", length = 10)
    private String languagePreference;

    @Column(name = "marketing_agreed", nullable = false)
    private boolean marketingAgreed;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected User() {
    }

    public User(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        this.phoneVerified = false;
        this.status = UserStatus.ACTIVE;
        this.role = UserRole.USER;
        this.languagePreference = "ko";
        this.marketingAgreed = false;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getLanguagePreference() {
        return languagePreference;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void updateProfile(String nickname, String languagePreference) {
        this.nickname = nickname;
        this.languagePreference = languagePreference;
    }

    public void verifyPhone(String phone) {
        this.phone = phone;
        this.phoneVerified = true;
    }

    public UserStatus getStatus() {
        return status;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }
}
