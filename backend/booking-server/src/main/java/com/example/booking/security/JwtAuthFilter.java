package com.example.booking.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/dev-login",
            "/api/v1/auth/refresh",
            "/api/v1/venues/**",
            "/api/v1/rooms/**",
            "/error"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // Skip public paths
        if (PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            chain.doFilter(req, res);
            return;
        }

        // Validate JWT
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":2001,\"message\":\"未登录\"}");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validate(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":2001,\"message\":\"登录已过期\"}");
            return;
        }

        UserContext.setUserId(jwtTokenProvider.getUserId(token));
        UserContext.setOpenid(jwtTokenProvider.getOpenid(token));

        try {
            chain.doFilter(req, res);
        } finally {
            UserContext.clear();
        }
    }
}
