package com.tradev.domain.ai.service;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.ai.client.ClaudeWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ItemDescriptionService {

    private final ClaudeWebClient claudeWebClient;
    private final AiRateLimitService rateLimitService;

    private static final String SYSTEM_PROMPT = """
        당신은 중고거래 플랫폼의 상품 설명 작성 도우미입니다.
        사용자가 제공한 상품명과 카테고리를 바탕으로 매력적이고 신뢰감 있는 상품 설명을 작성해주세요.

        규칙:
        - 200자 내외로 작성
        - 상품의 상태, 특징, 사용감을 자연스럽게 표현
        - 과장되거나 허위인 내용 금지
        - 한국어로 작성
        - 마크다운 형식 사용 금지
        """;

    public Mono<String> generate(Long userId, String title, String categoryName) {
        if (!rateLimitService.tryConsume(userId)) {
            throw new TradevException(ErrorCode.AI_DAILY_LIMIT_EXCEEDED);
        }

        String userMessage = String.format(
            "상품명: %s\n카테고리: %s\n\n위 상품에 대한 중고거래 설명을 작성해주세요.",
            title, categoryName
        );

        return claudeWebClient.complete(SYSTEM_PROMPT, userMessage);
    }
}
