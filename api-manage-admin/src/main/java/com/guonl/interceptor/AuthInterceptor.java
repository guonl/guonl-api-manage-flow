package com.guonl.interceptor;

import com.guonl.common.ApiResult;
import com.guonl.common.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * 登录认证拦截器：未登录访问 /api/** 返回 401 JSON
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(Constants.SESSION_USER) != null) {
            return true;
        }
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON_MAPPER.writeValueAsString(ApiResult.fail(401, "未登录或登录已过期")));
        return false;
    }
}
