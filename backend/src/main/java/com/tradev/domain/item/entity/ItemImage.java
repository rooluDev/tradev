package com.tradev.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 500)
    private String s3Key;

    @Column(nullable = false)
    private int sortOrder;

    public ItemImage(Item item, String imageUrl, String s3Key, int sortOrder) {
        this.item = item;
        this.imageUrl = imageUrl;
        this.s3Key = s3Key;
        this.sortOrder = sortOrder;
    }
}
