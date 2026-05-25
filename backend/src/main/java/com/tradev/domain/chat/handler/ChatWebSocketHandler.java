package com.tradev.domain.chat.handler;

import com.tradev.domain.chat.dto.ChatMessageResponse;
import com.tradev.domain.chat.dto.SendMessageRequest;
import com.tradev.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket STOMP 메시지 핸들러
 *
 * 클라이언트 → 서버: /app/chat/{roomId}/send
 * 서버 → 구독자:    /topic/chat/{roomId}
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final ChatService chatService;

    @MessageMapping("/chat/{roomId}/send")
    public void handleMessage(
        @DestinationVariable Long roomId,
        @Payload SendMessageRequest request,
        Principal principal
    ) {
        if (principal == null) {
            log.warn("[WS] 인증되지 않은 메시지 무시 (roomId={})", roomId);
            return;
        }
        Long senderId = Long.valueOf(principal.getName());
        ChatMessageResponse response = chatService.sendMessage(senderId, roomId, request);
        log.debug("[WS] 메시지 전송 roomId={} senderId={}", roomId, senderId);
        // chatService.sendMessage 내부에서 messagingTemplate.convertAndSend 처리
    }
}
