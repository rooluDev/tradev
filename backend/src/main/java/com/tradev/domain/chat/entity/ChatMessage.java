package com.tradev.domain.chat.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages",
    indexes = {
        @Index(name = "idx_chatmessage_room_created", columnList = "room_id, created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type = MessageType.TEXT;

    @Column(nullable = false, length = 1000)
    private String content;

    @Builder
    public ChatMessage(ChatRoom room, User sender, MessageType type, String content) {
        this.room = room;
        this.sender = sender;
        this.type = type;
        this.content = content;
    }
}
