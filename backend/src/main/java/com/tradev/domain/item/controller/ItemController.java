package com.tradev.domain.item.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.item.dto.*;
import com.tradev.domain.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemDetailResponse>> createItem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(itemService.createItem(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ItemSummary>>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String tradeType,
            @RequestParam(required = false) String itemCondition,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize) {

        ItemSearchCondition.ItemSearchConditionBuilder builder = ItemSearchCondition.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .tradeType(tradeType)
                .itemCondition(itemCondition)
                .status(status)
                .sellerId(sellerId)
                .pageSize(pageSize);

        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                builder.cursorCreatedAt(LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                builder.cursorId(Long.parseLong(parts[1]));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(itemService.searchItems(builder.build())));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemDetailResponse>> getItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItem(itemId, userId)));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemDetailResponse>> updateItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(itemService.updateItem(itemId, userId, request)));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        itemService.deleteItem(itemId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{itemId}/boost")
    public ResponseEntity<ApiResponse<Void>> boostItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        itemService.boostItem(itemId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{itemId}/visibility")
    public ResponseEntity<ApiResponse<Void>> toggleVisibility(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        itemService.toggleVisibility(itemId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
