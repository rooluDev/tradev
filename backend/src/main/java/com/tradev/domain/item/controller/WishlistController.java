package com.tradev.domain.item.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.item.dto.ItemSummary;
import com.tradev.domain.item.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/api/items/{itemId}/wishlist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleWishlist(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.toggleWishlist(userId, itemId)));
    }

    @GetMapping("/api/users/me/wishlist")
    public ResponseEntity<ApiResponse<CursorPageResponse<ItemSummary>>> getMyWishlist(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlist(userId, cursor, pageSize)));
    }
}
