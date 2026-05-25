package com.tradev.domain.chat.dto;

import com.tradev.domain.chat.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotNull(message = "메시지 타입은 필수입니다")
    MessageType type,

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 1000, message = "메시지는 1000자 이내여야 합니다")
    String content
) {}
