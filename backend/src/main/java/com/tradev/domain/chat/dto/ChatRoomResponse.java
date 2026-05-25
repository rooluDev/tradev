package com.tradev.domain.chat.dto;

import com.tradev.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
    Long id,
    ItemInfo item,
    UserInfo opponent,   // 상대방 정보 (요청한 사용자 기준)
    String lastMessage,
    int unreadCount,
    LocalDateTime updatedAt
) {
    public record ItemInfo(Long id, String title, String thumbnailUrl) {}
    public record UserInfo(Long id, String nickname, String profileImageUrl) {}

    public static ChatRoomResponse from(ChatRoom room, Long myUserId) {
        boolean isBuyer = room.getBuyer().getId().equals(myUserId);
        var opponent = isBuyer ? room.getSeller() : room.getBuyer();

        return new ChatRoomResponse(
            room.getId(),
            new ItemInfo(
                room.getItem().getId(),
                room.getItem().getTitle(),
                room.getItem().getThumbnailUrl()
            ),
            new UserInfo(
                opponent.getId(),
                opponent.getNickname(),
                opponent.getProfileImageUrl()
            ),
            room.getLastMessage(),
            room.getUnreadCount(myUserId),
            room.getUpdatedAt()
        );
    }
}
