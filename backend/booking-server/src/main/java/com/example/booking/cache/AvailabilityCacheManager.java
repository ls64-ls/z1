package com.example.booking.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AvailabilityCacheManager {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "cache:avail:";
    private static final Duration TTL = Duration.ofMinutes(5);

    public void setAvailability(Long roomId, String date, String json) {
        String key = PREFIX + roomId + ":" + date;
        redisTemplate.opsForValue().set(key, json, TTL);
    }

    public String getAvailability(Long roomId, String date) {
        return redisTemplate.opsForValue().get(PREFIX + roomId + ":" + date);
    }

    public void evictRoomAvailability(Long roomId, String date) {
        redisTemplate.delete(PREFIX + roomId + ":" + date);
    }
}
