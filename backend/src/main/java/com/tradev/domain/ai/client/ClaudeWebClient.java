package com.tradev.domain.ai.client;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeWebClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;

    public ClaudeWebClient(
        @Value("${claude.api-key}") String apiKey,
        @Value("${claude.model:claude-sonnet-4-6}") String model,
        @Value("${claude.max-tokens:1024}") int maxTokens
    ) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
            .baseUrl(API_URL)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }

    /**
     * 일반 단건 응답
     */
    public Mono<String> complete(String systemPrompt, String userMessage) {
        Map<String, Object> body = buildRequestBody(systemPrompt, userMessage, false);
        return webClient.post()
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(30))
            .map(resp -> {
                var content = (List<?>) resp.get("content");
                if (content == null || content.isEmpty()) {
                    throw new TradevException(ErrorCode.AI_GENERATION_FAILED);
                }
                return (String) ((Map<?, ?>) content.get(0)).get("text");
            })
            .onErrorMap(e -> {
                if (e instanceof TradevException) return e;
                log.error("[Claude] API 호출 실패: {}", e.getMessage());
                return new TradevException(ErrorCode.AI_GENERATION_FAILED);
            });
    }

    /**
     * 스트리밍 응답 (SSE용)
     * 각 청크는 "text_delta" 이벤트의 text 값
     */
    public Flux<String> stream(String systemPrompt, String userMessage) {
        Map<String, Object> body = buildRequestBody(systemPrompt, userMessage, true);
        return webClient.post()
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .timeout(Duration.ofSeconds(60))
            .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
            .map(line -> line.substring(6))   // "data: " 제거
            .filter(json -> json.contains("\"text_delta\""))
            .map(json -> {
                // {"type":"content_block_delta","delta":{"type":"text_delta","text":"..."}}
                int idx = json.indexOf("\"text\":\"");
                if (idx == -1) return "";
                int start = idx + 8;
                int end = json.indexOf("\"", start);
                return end == -1 ? "" : json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
            })
            .filter(text -> !text.isEmpty())
            .onErrorMap(e -> {
                if (e instanceof TradevException) return e;
                log.error("[Claude] 스트리밍 실패: {}", e.getMessage());
                return new TradevException(ErrorCode.AI_GENERATION_FAILED);
            });
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userMessage,
                                                   boolean stream) {
        return Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "stream", stream,
            "messages", List.of(Map.of("role", "user", "content", userMessage))
        );
    }
}
