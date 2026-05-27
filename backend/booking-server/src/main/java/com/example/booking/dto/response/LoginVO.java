package com.example.booking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private String token;
    private long expiresIn;
    private UserInfoVO user;

    @Data
    @Builder
    public static class UserInfoVO {
        private Long id;
        private String nickname;
        private String avatarUrl;
        private String phone;
    }
}
