package com.tradev.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * SSE 연결 생성 및 등록
     */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitterMap.remove(userId);
            log.debug("[SSE] 연결 완료 (userId={})", userId);
        });
        emitter.onTimeout(() -> {
            emitterMap.remove(userId);
            log.debug("[SSE] 연결 타임아웃 (userId={})", userId);
        });
        emitter.onError(e -> {
            emitterMap.remove(userId);
            log.debug("[SSE] 연결 오류 (userId={}): {}", userId, e.getMessage());
        });

        emitterMap.put(userId, emitter);

        // 연결 직후 더미 이벤트 (nginx 버퍼링 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitterMap.remove(userId);
            log.warn("[SSE] 초기 이벤트 전송 실패 (userId={})", userId);
        }

        log.debug("[SSE] 새 연결 (userId={}), 현재 연결 수: {}", userId, emitterMap.size());
        return emitter;
    }

    /**
     * 특정 사용자에게 알림 이벤트 전송
     */
    public void send(Long userId, Object data) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(data));
        } catch (IOException e) {
            emitterMap.remove(userId);
            log.warn("[SSE] 전송 실패, 연결 제거 (userId={})", userId);
        }
    }

    /**
     * 연결 중인지 확인
     */
    public boolean isConnected(Long userId) {
        return emitterMap.containsKey(userId);
    }

    /**
     * 현재 연결 수 조회 (모니터링용)
     */
    public int getConnectionCount() {
        return emitterMap.size();
    }
}
