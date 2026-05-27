package com.example.booking.service;

import com.example.booking.entity.User;
import com.example.booking.repository.mapper.UserMapper;
import com.example.booking.security.JwtTokenProvider;
import com.example.booking.security.WeChatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WeChatAuthService weChatAuthService;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public Map<String, Object> login(String code) {
        WeChatAuthService.WeChatSession session = weChatAuthService.code2Session(code);

        User user = userMapper.findByOpenid(session.openid());
        if (user == null) {
            user = new User();
            user.setOpenid(session.openid());
            user.setUnionid(session.unionid());
            user.setStatus("ACTIVE");
            userMapper.insert(user);
            log.info("Created new user: id={}, openid={}", user.getId(), session.openid());
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getOpenid());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        return result;
    }

    public Map<String, Object> devLogin(String openid) {
        User user = userMapper.findByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("DevUser");
            user.setStatus("ACTIVE");
            userMapper.insert(user);
            log.info("Dev login - created user: id={}, openid={}", user.getId(), openid);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getOpenid());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        return result;
    }

    public String refreshToken(Long userId, String openid) {
        return jwtTokenProvider.generateToken(userId, openid);
    }

    public User getProfile(Long userId) {
        return userMapper.selectById(userId);
    }
}
