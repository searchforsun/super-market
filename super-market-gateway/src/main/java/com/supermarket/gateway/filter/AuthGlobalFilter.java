package com.supermarket.gateway.filter;

import com.supermarket.common.core.constants.GlobalConstants;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.common.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> WHITELIST = List.of(
        "/api/user/register",
        "/api/user/login",
        "/api/auth/login",
        "/api/search",
        "/api/file/raw",
        "/actuator",
        "/doc.html",
        "/swagger-ui",
        "/webjars",
        "/v3/api-docs",
        "/favicon.ico"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (WHITELIST.stream().anyMatch(path::startsWith)
            || path.contains("v3/api-docs")
            || path.contains("swagger-ui")
            || path.contains("webjars")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未提供认证 Token");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validate(token)) {
            return unauthorized(exchange, "Token 无效或已过期");
        }

        if (jwtUtil.isExpired(token)) {
            return unauthorized(exchange, "Token 已过期");
        }

        Long userId = jwtUtil.getUserId(token);
        List<String> roles = jwtUtil.getRoles(token);

        ServerHttpRequest mutatedRequest = request.mutate()
            .header(GlobalConstants.USER_ID, String.valueOf(userId))
            .header(GlobalConstants.USER_ROLES, String.join(",", roles))
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":%d,\"message\":\"%s\"}", ResultCode.UNAUTHORIZED.getCode(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
