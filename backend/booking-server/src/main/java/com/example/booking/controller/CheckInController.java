package com.example.booking.controller;

import com.example.booking.common.Result;
import com.example.booking.security.UserContext;
import com.example.booking.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping
    public Result<String> checkIn(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Long bookingId = getLong(body, "bookingId");
        checkInService.checkIn(bookingId, userId);
        return Result.success();
    }

    @PostMapping("/checkout")
    public Result<String> checkOut(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Long bookingId = getLong(body, "bookingId");
        checkInService.checkOut(bookingId, userId);
        return Result.success();
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        return val != null ? Long.parseLong(val.toString()) : null;
    }
}
