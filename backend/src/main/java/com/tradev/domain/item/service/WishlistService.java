package com.tradev.domain.item.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.item.dto.ItemSummary;
import com.tradev.domain.item.entity.Item;
import com.tradev.domain.item.entity.Wishlist;
import com.tradev.domain.item.repository.ItemRepository;
import com.tradev.domain.item.repository.WishlistRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> toggleWishlist(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndItemId(userId, itemId);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            item.removeWish();
            return Map.of("wished", false, "wishCount", item.getWishCount());
        } else {
            wishlistRepository.save(new Wishlist(user, item));
            item.addWish();
            return Map.of("wished", true, "wishCount", item.getWishCount());
        }
    }

    public CursorPageResponse<ItemSummary> getMyWishlist(Long userId, String cursor, int pageSize) {
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;

        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorCreatedAt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<Wishlist> wishlists = wishlistRepository.findByUserIdWithCursor(
                userId, cursorCreatedAt, cursorId, pageSize + 1
        );

        // Wishlist -> ItemSummary 변환 (간략)
        List<ItemSummary> items = wishlists.stream().map(w -> {
            Item item = w.getItem();
            ItemSummary summary = new ItemSummary();
            summary.setId(item.getId());
            summary.setTitle(item.getTitle());
            summary.setPrice(item.getPrice());
            summary.setStatus(item.getStatus().name());
            summary.setWishCount(item.getWishCount());
            summary.setViewCount(item.getViewCount());
            summary.setLocation(item.getLocation());
            summary.setCreatedAt(w.getCreatedAt());
            return summary;
        }).toList();

        return CursorPageResponse.of(items, pageSize, item ->
                item.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + item.getId()
        );
    }
}
