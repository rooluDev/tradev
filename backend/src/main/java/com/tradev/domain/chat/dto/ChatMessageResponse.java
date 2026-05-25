package com.tradev.domain.chat.dto;

import com.tradev.domain.chat.entity.ChatMessage;
import com.tradev.domain.chat.entity.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long roomId,
    SenderInfo sender,
    MessageType type,
    String content,
    LocalDateTime createdAt
) {
    public record SenderInfo(Long id, String nickname, String profileImageUrl) {}

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
            message.getId(),
            message.getRoom().getId(),
            new SenderInfo(
                message.getSender().getId(),
                message.getSender().getNickname(),
                message.getSender().getProfileImageUrl()
            ),
            message.getType(),
            message.getContent(),
            message.getCreatedAt()
        );
    }
}
