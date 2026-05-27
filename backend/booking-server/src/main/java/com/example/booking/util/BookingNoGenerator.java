package com.example.booking.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class BookingNoGenerator {

    private final StringRedisTemplate redisTemplate;

    public String generate(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "booking:seq:" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        redisTemplate.expire(redisKey, Duration.ofDays(2));
        return "BK" + dateStr + String.format("%04d", seq);
    }
}
