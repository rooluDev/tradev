package com.tradev.domain.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.frontend-url:https://tradev.shop}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = oAuth2User.getAttribute("userId");

        if (userId == null) {
            log.error("OAuth2 success but userId is null");
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=oauth2");
            return;
        }

        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        // Redis에 refresh token 저장
        redisTemplate.opsForValue().set(
                "refresh:" + userId,
                refreshToken,
                jwtUtil.getRefreshExpirySeconds(),
                TimeUnit.SECONDS
        );

        // Refresh token을 HttpOnly 쿠키로 설정
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge((int) jwtUtil.getRefreshExpirySeconds());
        response.addCookie(refreshCookie);

        // 프론트엔드 콜백 페이지로 리다이렉트 (access token을 query param으로 전달)
        String redirectUrl = frontendUrl + "/oauth2/callback?token=" + accessToken;
        log.info("OAuth2 login success for userId={}, redirecting to frontend", userId);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
