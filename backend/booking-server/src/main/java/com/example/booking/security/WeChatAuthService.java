package com.example.booking.security;

import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatAuthService {

    @Value("${wechat.miniapp.appid}")
    private String appId;

    @Value("${wechat.miniapp.secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public WeChatSession code2Session(String code) {
        // Validate credentials are configured
        if ("your-appid".equals(appId) || "your-secret".equals(appSecret)) {
            log.error("WeChat credentials not configured. appid and secret are still placeholder values.");
            throw new BusinessException(ErrorCode.WECHAT_AUTH_FAILED,
                    "微信登录未配置，请在application.yml中设置wechat.miniapp.appid和secret，或设置环境变量WX_APPID和WX_SECRET");
        }

        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + appId
                + "&secret=" + appSecret
                + "&js_code=" + code
                + "&grant_type=authorization_code";

        log.debug("Calling WeChat code2session with appid={}...", appId);

        WeChatSessionResponse resp;
        try {
            resp = restTemplate.getForObject(url, WeChatSessionResponse.class);
        } catch (Exception e) {
            log.error("WeChat API call failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.WECHAT_AUTH_FAILED, "微信服务器连接失败，请检查网络配置");
        }

        if (resp == null) {
            log.error("WeChat auth returned null response");
            throw new BusinessException(ErrorCode.WECHAT_AUTH_FAILED, "微信登录失败：服务器返回为空");
        }

        if (resp.getErrcode() != 0) {
            String errDetail = resp.getErrmsg();
            log.error("WeChat auth failed: errcode={}, errmsg={}, appid={}",
                    resp.getErrcode(), errDetail, appId);
            throw new BusinessException(ErrorCode.WECHAT_AUTH_FAILED,
                    "微信登录失败: " + (errDetail != null ? errDetail : "未知错误"));
        }

        log.info("WeChat auth success for openid={}", resp.getOpenid());
        return new WeChatSession(resp.getOpenid(), resp.getUnionid(), resp.getSessionKey());
    }

    @Data
    public static class WeChatSessionResponse {
        private String openid;
        private String sessionKey;
        private String unionid;
        private int errcode;
        private String errmsg;

        @JsonProperty("session_key")
        public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    }

    public record WeChatSession(String openid, String unionid, String sessionKey) {}
}
