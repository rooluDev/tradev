package com.tradev.domain.notification.event;

import lombok.Getter;

@Getter
public class ChatMessageEvent {
    private final Long recipientId;
    private final String senderNickname;
    private final Long roomId;
    private final String contentPreview;

    public ChatMessageEvent(Long recipientId, String senderNickname, Long roomId, String contentPreview) {
        this.recipientId = recipientId;
        this.senderNickname = senderNickname;
        this.roomId = roomId;
        this.contentPreview = contentPreview;
    }
}
