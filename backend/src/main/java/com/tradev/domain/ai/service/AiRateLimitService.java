package com.tradev.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AiRateLimitService {

    private static final int DAILY_LIMIT = 10;
    private final StringRedisTemplate redisTemplate;

    /**
     * 사용 가능 여부 확인 후 카운터 증가
     * @return true: 사용 가능, false: 한도 초과
     */
    public boolean tryConsume(Long userId) {
        String key = "ai:daily:" + userId + ":" + LocalDate.now();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            // 최초 사용 → 자정까지 TTL 설정
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return count <= DAILY_LIMIT;
    }

    public int getUsageCount(Long userId) {
        String key = "ai:daily:" + userId + ":" + LocalDate.now();
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

    public int getRemainingCount(Long userId) {
        return Math.max(0, DAILY_LIMIT - getUsageCount(userId));
    }
}
