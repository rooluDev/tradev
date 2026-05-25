package com.tradev.domain.trade.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.trade.dto.TradeRequest;
import com.tradev.domain.trade.dto.TradeResponse;
import com.tradev.domain.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeResponse>> requestTrade(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.requestTrade(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<TradeResponse>>> getTrades(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.getTrades(userId, status, cursor, pageSize)));
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<ApiResponse<TradeResponse>> getTrade(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.getTrade(tradeId, userId)));
    }

    @PatchMapping("/{tradeId}/accept")
    public ResponseEntity<ApiResponse<TradeResponse>> acceptTrade(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.acceptTrade(tradeId, userId)));
    }

    @PatchMapping("/{tradeId}/reject")
    public ResponseEntity<ApiResponse<TradeResponse>> rejectTrade(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.rejectTrade(tradeId, userId)));
    }

    @PatchMapping("/{tradeId}/confirm")
    public ResponseEntity<ApiResponse<TradeResponse>> confirmTrade(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.confirmTrade(tradeId, userId)));
    }

    @PatchMapping("/{tradeId}/cancel")
    public ResponseEntity<ApiResponse<TradeResponse>> cancelTrade(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success(tradeService.cancelTrade(tradeId, userId, reason)));
    }
}
