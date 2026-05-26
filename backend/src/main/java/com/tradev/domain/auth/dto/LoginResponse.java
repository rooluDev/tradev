package com.tradev.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tradev.domain.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;

    // refresh token은 HttpOnly 쿠키로만 전달 — JSON 응답 바디에는 포함하지 않음
    @JsonIgnore
    private final String refreshToken;

    private final UserInfo user;

    public LoginResponse(String accessToken, String refreshToken, User user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = new UserInfo(user);
    }

    @Getter
    public static class UserInfo {
        private final Long id;
        private final String email;
        private final String nickname;
        private final String profileImageUrl;
        private final String role;
        private final int trustScore;
        private final String trustGrade;

        public UserInfo(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.nickname = user.getNickname();
            this.profileImageUrl = user.getProfileImageUrl();
            this.role = user.getRole().name();
            this.trustScore = user.getTrustScore();
            this.trustGrade = user.getTrustGrade().name();
        }
    }
}
