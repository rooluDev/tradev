package com.tradev.domain.ai.service;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.ai.client.ClaudeWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceRecommendationService {

    private final ClaudeWebClient claudeWebClient;
    private final AiRateLimitService rateLimitService;

    private static final String SYSTEM_PROMPT = """
        당신은 중고거래 가격 추천 전문가입니다.
        상품 정보를 분석하여 적정 중고 거래 가격을 추천해주세요.

        응답 형식 (JSON만, 다른 텍스트 없이):
        {"minPrice": 숫자, "maxPrice": 숫자, "recommendedPrice": 숫자, "reason": "근거 한 줄"}

        규칙:
        - 가격은 원(KRW) 단위 정수
        - 시장 중고 시세 기반으로 추천
        - reason은 50자 이내
        """;

    public Mono<PriceRecommendation> recommend(Long userId, String title,
                                                String categoryName, String description) {
        if (!rateLimitService.tryConsume(userId)) {
            throw new TradevException(ErrorCode.AI_DAILY_LIMIT_EXCEEDED);
        }

        String userMessage = String.format(
            "상품명: %s\n카테고리: %s\n설명: %s",
            title, categoryName, description != null ? description : ""
        );

        return claudeWebClient.complete(SYSTEM_PROMPT, userMessage)
            .map(this::parseRecommendation)
            .onErrorReturn(new PriceRecommendation(0, 0, 0, "가격 추천을 가져올 수 없습니다."));
    }

    private PriceRecommendation parseRecommendation(String json) {
        try {
            // 간단한 파싱 (Jackson 없이)
            int min = extractInt(json, "minPrice");
            int max = extractInt(json, "maxPrice");
            int rec = extractInt(json, "recommendedPrice");
            String reason = extractString(json, "reason");
            return new PriceRecommendation(min, max, rec, reason);
        } catch (Exception e) {
            log.warn("[AI] 가격 추천 파싱 실패: {}", json);
            return new PriceRecommendation(0, 0, 0, "가격 추천을 파싱할 수 없습니다.");
        }
    }

    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\": ";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0;
        int start = idx + pattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Integer.parseInt(json.substring(start, end).trim());
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\": \"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    public record PriceRecommendation(int minPrice, int maxPrice,
                                       int recommendedPrice, String reason) {}
}
