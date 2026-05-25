package com.tradev.domain.user.dto;

import com.tradev.domain.user.entity.User;
import lombok.Getter;

@Getter
public class MyInfoResponse extends UserResponse {

    private final String email;
    private final String role;
    private final Boolean emailVerified;

    public MyInfoResponse(User user) {
        super(user);
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.emailVerified = user.getEmailVerified();
    }
}
