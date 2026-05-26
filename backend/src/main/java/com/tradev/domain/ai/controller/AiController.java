package com.tradev.domain.ai.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.domain.ai.dto.PriceRecommendRequest;
import com.tradev.domain.ai.service.ItemDescriptionService;
import com.tradev.domain.ai.service.PriceRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ItemDescriptionService itemDescriptionService;
    private final PriceRecommendationService priceRecommendationService;

    /**
     * 상품 설명 자동완성 — SSE 스트리밍
     * 클라이언트는 EventSource 또는 fetch+ReadableStream으로 수신
     */
    @GetMapping(value = "/item-description", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateDescription(
        @AuthenticationPrincipal Long userId,
        @RequestParam String title,
        @RequestParam String categoryName
    ) {
        return itemDescriptionService.generateStream(userId, title, categoryName)
            .map(chunk -> "data: " + chunk + "\n\n");
    }

    /**
     * 가격 추천 — 단건 JSON 응답
     */
    @PostMapping("/price-recommendation")
    public Mono<ResponseEntity<ApiResponse<PriceRecommendationService.PriceRecommendation>>> recommendPrice(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody PriceRecommendRequest request
    ) {
        return priceRecommendationService.recommend(userId, request.title(),
                request.categoryName(), request.description())
            .map(result -> ResponseEntity.ok(ApiResponse.success(result)));
    }
}
