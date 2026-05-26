package com.tradev.domain.ai.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.domain.ai.dto.PriceRecommendRequest;
import com.tradev.domain.ai.service.ItemDescriptionService;
import com.tradev.domain.ai.service.PriceRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ItemDescriptionService itemDescriptionService;
    private final PriceRecommendationService priceRecommendationService;

    /**
     * 상품 설명 자동완성 — 단건 JSON 응답
     */
    @GetMapping("/item-description")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> generateDescription(
        @AuthenticationPrincipal Long userId,
        @RequestParam String title,
        @RequestParam String categoryName
    ) {
        return itemDescriptionService.generate(userId, title, categoryName)
            .map(text -> ResponseEntity.ok(ApiResponse.success(Map.of("description", text))));
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
