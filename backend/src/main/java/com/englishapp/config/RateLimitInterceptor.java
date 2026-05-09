package com.englishapp.config;

import com.englishapp.common.ApiException;
import com.englishapp.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int LIMIT_PER_MINUTE = 60;

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true; // unauthenticated — let security filter handle it
        }

        String token = authHeader.substring(7);
        String userId;
        try {
            userId = jwtService.extractEmail(token);
        } catch (Exception e) {
            return true; // invalid token — let security filter handle it
        }

        long minute = Instant.now().getEpochSecond() / 60;
        String key = "rl:api:" + userId + ":" + minute;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(70)); // slight buffer over 60s
        }

        if (count != null && count > LIMIT_PER_MINUTE) {
            throw ApiException.tooManyRequests("Rate limit exceeded — max " + LIMIT_PER_MINUTE + " requests per minute");
        }

        return true;
    }
}
