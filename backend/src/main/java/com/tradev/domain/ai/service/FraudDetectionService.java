package com.tradev.domain.ai.service;

import com.tradev.domain.ai.client.ClaudeWebClient;
import com.tradev.domain.notification.entity.NotificationType;
import com.tradev.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final ClaudeWebClient claudeWebClient;
    private final NotificationService notificationService;

    private static final Long ADMIN_USER_ID = 1L; // 관리자 계정 ID (실제 환경에서 설정)

    private static final String SYSTEM_PROMPT = """
        당신은 중고거래 사기 패턴 감지 시스템입니다.
        상품 설명을 분석하여 사기 가능성을 평가해주세요.

        응답 형식 (JSON만):
        {"isSuspicious": true/false, "confidence": 0.0~1.0, "reason": "근거"}

        사기 패턴 예시: 과도한 할인, 불가능한 조건, 선입금 요구 암시, 금지 물품 등
        """;

    /**
     * 상품 등록 시 비동기로 사기 패턴 감지
     * 의심 감지 시 관리자에게 알림
     */
    @Async
    public void detectAsync(Long itemId, String title, String description) {
        if (description == null || description.isBlank()) return;

        String userMessage = String.format("상품명: %s\n설명: %s", title, description);

        claudeWebClient.complete(SYSTEM_PROMPT, userMessage)
            .subscribe(
                json -> {
                    if (json.contains("\"isSuspicious\": true") ||
                        json.contains("\"isSuspicious\":true")) {
                        log.warn("[FraudDetection] 의심 상품 감지 itemId={}", itemId);
                        // 관리자 알림
                        notificationService.createAndSend(
                            ADMIN_USER_ID,
                            NotificationType.REPORT_PROCESSED,
                            String.format("사기 의심 상품이 감지되었습니다. (상품 ID: %d)", itemId),
                            "/admin/items/" + itemId
                        );
                    }
                },
                error -> log.warn("[FraudDetection] 감지 실패 itemId={}: {}", itemId, error.getMessage())
            );
    }
}
