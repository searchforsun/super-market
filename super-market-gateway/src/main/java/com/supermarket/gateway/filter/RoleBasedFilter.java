package com.supermarket.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class RoleBasedFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITELIST = List.of(
        "/api/user/register",
        "/api/user/login",
        "/api/auth/login",
        "/api/search",
        "/actuator",
        "/doc.html",
        "/swagger-ui",
        "/webjars",
        "/v3/api-docs",
        "/favicon.ico"
    );

    private static final Map<String, Set<String>> PATH_ROLE_MAP = Map.of(
        "/api/platform/admin", Set.of("ROLE_ADMIN"),
        "/api/coupon/admin",   Set.of("ROLE_ADMIN", "ROLE_MERCHANT"),
        "/api/seckill/admin",  Set.of("ROLE_ADMIN", "ROLE_MERCHANT"),
        "/api/shop/merchant",  Set.of("ROLE_MERCHANT"),
        "/api/order/admin",    Set.of("ROLE_ADMIN")
    );

    private static final Set<String> ALL_ROLES = Set.of("ROLE_USER", "ROLE_MERCHANT", "ROLE_ADMIN");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单不过滤
        if (WHITELIST.stream().anyMatch(path::startsWith)
            || path.contains("v3/api-docs")
            || path.contains("swagger-ui")
            || path.contains("webjars")) {
            return chain.filter(exchange);
        }

        String rolesHeader = request.getHeaders().getFirst("X-User-Roles");
        Set<String> userRoles = (rolesHeader != null && !rolesHeader.isEmpty())
            ? Set.of(rolesHeader.split(","))
            : Set.of();

        // 匹配路径→角色
        for (var entry : PATH_ROLE_MAP.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                boolean hasRole = userRoles.stream().anyMatch(entry.getValue()::contains);
                if (!hasRole) {
                    return forbidden(exchange, "权限不足: 需要 " + entry.getValue());
                }
                return chain.filter(exchange);
            }
        }

        // 其余路径仅需登录（至少有一个角色）
        if (userRoles.isEmpty() || userRoles.stream().noneMatch(ALL_ROLES::contains)) {
            return forbidden(exchange, "未授权访问");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":403,\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
