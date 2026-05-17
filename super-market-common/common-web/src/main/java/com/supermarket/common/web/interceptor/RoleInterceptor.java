package com.supermarket.common.web.interceptor;

import com.supermarket.common.core.constants.GlobalConstants;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.common.web.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequireRole annotation = hm.getMethodAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (annotation == null) return true; // 无注解，Gateway 兜底

        String rolesHeader = request.getHeader(GlobalConstants.USER_ROLES);
        Set<String> userRoles = (rolesHeader != null && !rolesHeader.isEmpty())
            ? Set.of(rolesHeader.split(","))
            : Set.of();

        boolean hasRole = Arrays.stream(annotation.value()).anyMatch(userRoles::contains);
        if (!hasRole) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return true;
    }
}
