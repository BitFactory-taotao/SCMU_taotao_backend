package com.bit.scmu_taotao.interceptor;

import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 登录拦截器
 * 校验请求头中的 Token 是否有效
 */
@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtil tokenUtil;

    // ObjectMapper 作为工具类直接实例化，无需依赖 Spring 容器
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 Token
        String token = request.getHeader("Authorization");
        log.info("收到请求：{}，Token：{}", request.getRequestURI(), token);

        // 2. 校验 Token
        if (token != null && !token.isEmpty()) {
            // 去除 "Bearer " 前缀
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String userId = tokenUtil.validateToken(cleanToken);

            if (userId != null) {
                // Token 有效，存入 ThreadLocal
                UserContext.setUserId(userId);
                return true;
            }
        }

        // 3. Token 无效，返回 401
        log.warn("Token 无效或缺失，拦截请求：{}", request.getRequestURI());
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Result result = Result.fail(401, "未登录或登录已过期");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(result));
            writer.flush();
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.remove();
    }
}
