package com.tradev.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class SignupRequest {

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*]{8,20}$",
            message = "비밀번호는 영문+숫자를 포함해야 합니다.")
    private String password;

    @NotBlank
    @Size(min = 2, max = 15, message = "닉네임은 2~15자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 밑줄만 사용 가능합니다.")
    private String nickname;
}
