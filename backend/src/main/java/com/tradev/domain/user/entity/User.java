package com.tradev.domain.user.entity;

import com.tradev.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 200)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private int trustScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrustGrade trustGrade = TrustGrade.SEED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OAuthProvider oauthProvider;

    @Column(length = 255)
    private String oauthProviderId;

    private Boolean emailVerified = false;

    private LocalDateTime suspendedUntil;

    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String nickname, OAuthProvider oauthProvider, String oauthProviderId, Boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.oauthProvider = oauthProvider;
        this.oauthProviderId = oauthProviderId;
        this.emailVerified = emailVerified != null ? emailVerified : false;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updateProfile(String nickname, String bio, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (bio != null) this.bio = bio;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void suspend(LocalDateTime until) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    public void addTrustScore(int delta) {
        this.trustScore = Math.max(0, Math.min(100, this.trustScore + delta));
        this.trustGrade = TrustGrade.fromScore(this.trustScore);
    }

    public boolean isActive() {
        if (status == UserStatus.SUSPENDED && suspendedUntil != null && LocalDateTime.now().isAfter(suspendedUntil)) {
            activate();
        }
        return status == UserStatus.ACTIVE;
    }
}
