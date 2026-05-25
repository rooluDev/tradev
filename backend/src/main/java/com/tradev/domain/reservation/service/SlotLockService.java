package com.tradev.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SlotLockService {

    private static final String SLOT_LOCK_PREFIX = "slot:lock:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    /**
     * 슬롯 임시 잠금 획득 (Redis SETNX)
     * @return true: 잠금 성공, false: 이미 잠긴 슬롯
     */
    public boolean tryLock(Long slotId, Long userId) {
        String key = SLOT_LOCK_PREFIX + slotId;
        String value = String.valueOf(userId);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, LOCK_TTL);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 슬롯 잠금 해제
     */
    public void unlock(Long slotId) {
        redisTemplate.delete(SLOT_LOCK_PREFIX + slotId);
    }

    /**
     * 슬롯이 잠겨있는지 확인
     */
    public boolean isLocked(Long slotId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SLOT_LOCK_PREFIX + slotId));
    }

    /**
     * 특정 사용자가 잠금을 보유하고 있는지 확인
     */
    public boolean isLockedBy(Long slotId, Long userId) {
        String value = redisTemplate.opsForValue().get(SLOT_LOCK_PREFIX + slotId);
        return String.valueOf(userId).equals(value);
    }

    /**
     * 잠금 TTL 연장 (예약 진행 중 만료 방지)
     */
    public void extendLock(Long slotId) {
        redisTemplate.expire(SLOT_LOCK_PREFIX + slotId, LOCK_TTL);
    }
}
