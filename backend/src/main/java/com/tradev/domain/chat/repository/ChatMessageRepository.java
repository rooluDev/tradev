package com.tradev.domain.chat.repository;

import com.tradev.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT cm FROM ChatMessage cm
        JOIN FETCH cm.sender
        WHERE cm.room.id = :roomId
          AND (:cursorCreatedAt IS NULL OR cm.createdAt < :cursorCreatedAt
               OR (cm.createdAt = :cursorCreatedAt AND cm.id < :cursorId))
        ORDER BY cm.createdAt DESC, cm.id DESC
        LIMIT :pageSize
        """)
    List<ChatMessage> findByRoomWithCursor(
        @Param("roomId") Long roomId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );
}
