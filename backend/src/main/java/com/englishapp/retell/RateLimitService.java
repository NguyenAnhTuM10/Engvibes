package com.englishapp.retell;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(UUID userId, String action, int dailyLimit) {
        String key = String.format("rate:%s:%s:%s", action, userId, LocalDate.now());
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return count != null && count <= dailyLimit;
    }
}
