package com.tradev.domain.user.dto;

import com.tradev.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final String nickname;
    private final String profileImageUrl;
    private final String bio;
    private final int trustScore;
    private final String trustGrade;
    private final String trustGradeLabel;
    private final String trustGradeIcon;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.bio = user.getBio();
        this.trustScore = user.getTrustScore();
        this.trustGrade = user.getTrustGrade().name();
        this.trustGradeLabel = user.getTrustGrade().getLabel();
        this.trustGradeIcon = user.getTrustGrade().getIcon();
        this.createdAt = user.getCreatedAt();
    }
}
