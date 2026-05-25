package com.tradev.domain.chat.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.chat.dto.ChatMessageResponse;
import com.tradev.domain.chat.dto.ChatRoomResponse;
import com.tradev.domain.chat.dto.SendMessageRequest;
import com.tradev.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 채팅방 생성 또는 조회 (상품 기준) */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getOrCreateRoom(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam Long itemId
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        ChatRoomResponse result = chatService.getOrCreateRoom(userId, itemId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result));
    }

    /** 내 채팅 목록 */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<CursorPageResponse<ChatRoomResponse>>> getMyChatRooms(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(
            ApiResponse.success(chatService.getMyChatRooms(userId, cursor, size))
        );
    }

    /** 메시지 목록 조회 */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long roomId,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "50") int size
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(
            ApiResponse.success(chatService.getMessages(userId, roomId, cursor, size))
        );
    }

    /** 메시지 전송 (REST fallback — 주 경로는 WebSocket) */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long roomId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        ChatMessageResponse result = chatService.sendMessage(userId, roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /** 읽음 처리 */
    @PatchMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long roomId
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        chatService.markAsRead(userId, roomId);
        return ResponseEntity.noContent().build();
    }
}
