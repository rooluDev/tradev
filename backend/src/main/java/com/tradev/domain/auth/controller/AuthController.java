package com.tradev.domain.auth.controller;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.auth.dto.*;
import com.tradev.domain.auth.service.AuthService;
import com.tradev.domain.auth.service.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request);
        setRefreshTokenCookie(response, loginResponse.getAccessToken());
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new TradevException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        LoginResponse loginResponse = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, loginResponse.getAccessToken());
        return ResponseEntity.ok(ApiResponse.success(Map.of("accessToken", loginResponse.getAccessToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {

        if (userId != null) {
            authService.logout(userId);
        }
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("available", authService.checkEmail(email))));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("available", authService.checkNickname(nickname))));
    }

    @PostMapping("/email-verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/email-verify/resend")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // prod에서는 true
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (jwtUtil.getRefreshExpirySeconds()));
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
