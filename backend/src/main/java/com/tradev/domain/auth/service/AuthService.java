package com.tradev.domain.auth.service;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.auth.dto.*;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String EMAIL_VERIFY_PREFIX = "email:verify:";
    private static final String PASSWORD_RESET_PREFIX = "password:reset:";
    private static final long VERIFY_TOKEN_TTL_HOURS = 24;
    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final AsyncMailService asyncMailService;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new TradevException(ErrorCode.USER_EMAIL_DUPLICATED);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new TradevException(ErrorCode.USER_NICKNAME_DUPLICATED);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .emailVerified(true)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new TradevException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new TradevException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!user.isActive()) {
            if (user.getStatus().name().equals("SUSPENDED")) {
                throw new TradevException(ErrorCode.USER_SUSPENDED);
            }
            throw new TradevException(ErrorCode.USER_WITHDRAWN);
        }

        return issueTokens(user);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        Long userId = jwtUtil.getUserId(refreshToken);
        String key = REFRESH_KEY_PREFIX + userId;
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null || !stored.equals(refreshToken)) {
            throw new TradevException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user);
    }

    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    public boolean checkEmail(String email) {
        return !userRepository.existsByEmail(email);
    }

    public boolean checkNickname(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public void verifyEmail(String token) {
        String key = EMAIL_VERIFY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new TradevException(ErrorCode.AUTH_VERIFY_TOKEN_INVALID);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new TradevException(ErrorCode.AUTH_EMAIL_ALREADY_VERIFIED);
        }

        user.verifyEmail();
        redisTemplate.delete(key);
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new TradevException(ErrorCode.AUTH_EMAIL_ALREADY_VERIFIED);
        }

        sendVerificationEmail(email);
    }

    public void requestPasswordReset(String email) {
        // 보안: 이메일 존재 여부 노출하지 않음
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            String key = PASSWORD_RESET_PREFIX + token;
            redisTemplate.opsForValue().set(key, email, RESET_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
            sendPasswordResetEmail(email, token);
        });
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        String key = PASSWORD_RESET_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new TradevException(ErrorCode.AUTH_RESET_TOKEN_INVALID);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(newPassword));
        redisTemplate.delete(key);
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        String key = REFRESH_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, refreshToken, jwtUtil.getRefreshExpirySeconds(), TimeUnit.SECONDS);

        return new LoginResponse(accessToken, refreshToken, user);
    }

    private void sendVerificationEmail(String email) {
        String token = UUID.randomUUID().toString();
        String key = EMAIL_VERIFY_PREFIX + token;
        redisTemplate.opsForValue().set(key, email, VERIFY_TOKEN_TTL_HOURS, TimeUnit.HOURS);

        asyncMailService.send(
            email,
            "[Tradev] 이메일 인증",
            "아래 링크를 클릭하여 이메일을 인증해주세요:\n\nhttps://tradev.shop/email-verify?token=" + token
        );
    }

    private void sendPasswordResetEmail(String email, String token) {
        asyncMailService.send(
            email,
            "[Tradev] 비밀번호 재설정",
            "아래 링크를 클릭하여 비밀번호를 재설정해주세요:\n\nhttps://tradev.shop/password-reset?token=" + token + "\n\n링크는 30분간 유효합니다."
        );
    }
}
