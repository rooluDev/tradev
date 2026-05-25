package com.tradev.domain.chat.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.chat.dto.ChatMessageResponse;
import com.tradev.domain.chat.dto.ChatRoomResponse;
import com.tradev.domain.chat.dto.SendMessageRequest;
import com.tradev.domain.chat.entity.ChatMessage;
import com.tradev.domain.chat.entity.ChatRoom;
import com.tradev.domain.chat.entity.MessageType;
import com.tradev.domain.chat.repository.ChatMessageRepository;
import com.tradev.domain.chat.repository.ChatRoomRepository;
import com.tradev.domain.item.entity.Item;
import com.tradev.domain.item.repository.ItemRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅방 생성 또는 기존 방 반환 (상품 + 구매자 기준 유니크)
     */
    @Transactional
    public ChatRoomResponse getOrCreateRoom(Long buyerId, Long itemId) {
        return chatRoomRepository.findByItemIdAndBuyerId(itemId, buyerId)
            .map(room -> ChatRoomResponse.from(room, buyerId))
            .orElseGet(() -> {
                Item item = itemRepository.findByIdWithDetails(itemId)
                    .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

                if (item.getSeller().getId().equals(buyerId)) {
                    throw new TradevException(ErrorCode.USER_CANNOT_SELF_TRADE);
                }

                User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

                ChatRoom room = ChatRoom.builder()
                    .item(item)
                    .buyer(buyer)
                    .seller(item.getSeller())
                    .build();
                chatRoomRepository.save(room);
                return ChatRoomResponse.from(room, buyerId);
            });
    }

    /**
     * 내 채팅 목록 (커서 페이지네이션, updatedAt 기준)
     */
    public CursorPageResponse<ChatRoomResponse> getMyChatRooms(Long userId, String cursor, int size) {
        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorTime = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<ChatRoom> rooms = chatRoomRepository.findByUserWithCursor(userId, cursorTime, cursorId, size + 1);
        List<ChatRoomResponse> responses = rooms.stream()
            .map(r -> ChatRoomResponse.from(r, userId))
            .collect(Collectors.toList());

        return CursorPageResponse.of(responses, size,
            r -> r.updatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());
    }

    /**
     * 메시지 목록 조회 (커서 페이지네이션)
     */
    public CursorPageResponse<ChatMessageResponse> getMessages(Long userId, Long roomId,
                                                                String cursor, int size) {
        ChatRoom room = getChatRoomWithAccess(roomId, userId);

        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorTime = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<ChatMessage> messages = chatMessageRepository.findByRoomWithCursor(
            roomId, cursorTime, cursorId, size + 1
        );
        List<ChatMessageResponse> responses = messages.stream()
            .map(ChatMessageResponse::from)
            .collect(Collectors.toList());

        return CursorPageResponse.of(responses, size,
            r -> r.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());
    }

    /**
     * 메시지 전송 (REST → WebSocket 브로드캐스트)
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, Long roomId, SendMessageRequest request) {
        ChatRoom room = getChatRoomWithAccess(roomId, senderId);
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
            .room(room)
            .sender(sender)
            .type(request.type())
            .content(request.content())
            .build();
        chatMessageRepository.save(message);

        // 채팅방 최종 메시지 + 미읽음 수 갱신
        room.updateLastMessage(truncate(request.content(), 100));
        room.incrementUnread(senderId);

        ChatMessageResponse response = ChatMessageResponse.from(message);

        // WebSocket 브로드캐스트: /topic/chat/{roomId}
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);

        return response;
    }

    /**
     * 채팅방 읽음 처리 (REST)
     */
    @Transactional
    public void markAsRead(Long userId, Long roomId) {
        ChatRoom room = getChatRoomWithAccess(roomId, userId);
        room.resetUnread(userId);
    }

    // ──────────── private ────────────

    private ChatRoom getChatRoomWithAccess(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findByIdWithDetails(roomId)
            .orElseThrow(() -> new TradevException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(userId)) {
            throw new TradevException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }
        return room;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "…";
    }
}
