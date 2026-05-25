package com.tradev.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 핸드셰이크 시 쿼리 파라미터 token=xxx 로 JWT 검증
 * 연결 속성에 userId를 주입 → ChatWebSocketHandler에서 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String query = request.getURI().getQuery();
            String token = UriComponentsBuilder.fromUriString("?" + (query != null ? query : ""))
                .build().getQueryParams().getFirst("token");

            if (token == null || token.isBlank()) {
                // Authorization 헤더에서도 확인
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token != null && jwtUtil.isAccessToken(token)) {
                Long userId = jwtUtil.getUserId(token);
                attributes.put("userId", userId);
                log.debug("[WS] Handshake OK userId={}", userId);
                return true;
            }
        } catch (Exception e) {
            log.warn("[WS] Handshake JWT 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
