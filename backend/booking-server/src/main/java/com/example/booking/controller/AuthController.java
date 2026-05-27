package com.example.booking.controller;

import com.example.booking.common.Result;
import com.example.booking.security.UserContext;
import com.example.booking.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error(1001, "缺少code参数");
        }
        Map<String, Object> result = authService.login(code);
        return Result.success(result);
    }

    @PostMapping("/refresh")
    public Result<String> refresh() {
        Long userId = UserContext.getUserId();
        String openid = UserContext.getOpenid();
        if (userId == null || openid == null) {
            return Result.error(2001, "未登录");
        }
        String token = authService.refreshToken(userId, openid);
        return Result.success(token);
    }

    @PostMapping("/dev-login")
    public Result<Map<String, Object>> devLogin(@RequestBody Map<String, String> body) {
        String openid = body.getOrDefault("openid", "dev_test_user");
        Map<String, Object> result = authService.devLogin(openid);
        return Result.success(result);
    }

    @GetMapping("/profile")
    public Result<com.example.booking.entity.User> profile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(2001, "未登录");
        }
        return Result.success(authService.getProfile(userId));
    }
}
