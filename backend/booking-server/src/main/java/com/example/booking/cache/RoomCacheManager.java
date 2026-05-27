package com.example.booking.cache;

import com.example.booking.dto.response.RoomVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCacheManager {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "cache:room:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public void setRoomList(Long venueId, int capacity, String roomsJson) {
        String key = PREFIX + "list:" + venueId + ":" + capacity;
        redisTemplate.opsForValue().set(key, roomsJson, TTL);
    }

    public String getRoomListFromCache(Long venueId, int capacity) {
        return redisTemplate.opsForValue().get(PREFIX + "list:" + venueId + ":" + capacity);
    }

    public void setRoomDetail(Long roomId, String roomJson) {
        String key = PREFIX + "detail:" + roomId;
        redisTemplate.opsForValue().set(key, roomJson, TTL);
    }

    public String getRoomDetailFromCache(Long roomId) {
        return redisTemplate.opsForValue().get(PREFIX + "detail:" + roomId);
    }

    public void evictRoom(Long roomId) {
        redisTemplate.delete(PREFIX + "detail:" + roomId);
        redisTemplate.delete(redisTemplate.keys(PREFIX + "list:*"));  // invalidate all list caches
    }
}
