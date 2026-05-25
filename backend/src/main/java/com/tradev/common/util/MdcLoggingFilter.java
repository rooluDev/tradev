package com.tradev.common.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 traceId / userId 를 MDC에 주입한다.
 * logback-spring.xml 의 JSON 인코더가 이 값을 자동으로 포함한다.
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY    = "traceId";
    private static final String USER_ID_KEY     = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1) traceId — 클라이언트 헤더 또는 서버 생성
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        try {
            MDC.put(TRACE_ID_KEY, traceId);

            // 2) userId — SecurityContext에서 (JWT 인증 후 세팅됨)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long userId) {
                MDC.put(USER_ID_KEY, String.valueOf(userId));
            }

            // 응답 헤더에도 traceId 포함 (디버깅 편의)
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(USER_ID_KEY);
        }
    }
}
