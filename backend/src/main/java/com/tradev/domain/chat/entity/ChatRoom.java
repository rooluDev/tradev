package com.tradev.domain.chat.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.item.entity.Item;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "buyer_id"}),
    indexes = {
        @Index(name = "idx_chatroom_buyer", columnList = "buyer_id"),
        @Index(name = "idx_chatroom_seller", columnList = "seller_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** 마지막 메시지 미리보기 */
    @Column(length = 200)
    private String lastMessage;

    /** 구매자 안 읽은 메시지 수 */
    @Column(nullable = false)
    private int buyerUnreadCount = 0;

    /** 판매자 안 읽은 메시지 수 */
    @Column(nullable = false)
    private int sellerUnreadCount = 0;

    @Builder
    public ChatRoom(Item item, User buyer, User seller) {
        this.item = item;
        this.buyer = buyer;
        this.seller = seller;
    }

    public void updateLastMessage(String message) {
        this.lastMessage = message;
    }

    public void incrementUnread(Long senderId) {
        // 보낸 사람이 구매자면 → 판매자의 미읽음 증가
        if (buyer.getId().equals(senderId)) {
            this.sellerUnreadCount++;
        } else {
            this.buyerUnreadCount++;
        }
    }

    public void resetUnread(Long readerId) {
        if (buyer.getId().equals(readerId)) {
            this.buyerUnreadCount = 0;
        } else {
            this.sellerUnreadCount = 0;
        }
    }

    public boolean isParticipant(Long userId) {
        return buyer.getId().equals(userId) || seller.getId().equals(userId);
    }

    public int getUnreadCount(Long userId) {
        if (buyer.getId().equals(userId)) return buyerUnreadCount;
        return sellerUnreadCount;
    }
}
