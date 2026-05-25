package com.tradev.domain.auth.dto;

import com.tradev.domain.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final UserInfo user;

    public LoginResponse(String accessToken, User user) {
        this.accessToken = accessToken;
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
