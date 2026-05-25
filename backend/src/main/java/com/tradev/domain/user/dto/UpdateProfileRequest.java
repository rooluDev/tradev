package com.tradev.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {

    @Size(min = 2, max = 15, message = "닉네임은 2~15자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 밑줄만 사용 가능합니다.")
    private String nickname;

    @Size(max = 200, message = "소개는 200자 이하여야 합니다.")
    private String bio;

    private String profileImageS3Key;
}
