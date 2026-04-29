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
import java.util.regex.Pattern;

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
        String method = request.getMethod();
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());

        // 1. 获取 Token
        String token = request.getHeader("Authorization");
        log.info("收到请求：{}，requestURI={}，path={}，Token={}", method, requestUri, path, token);

        // 放行预检请求，避免 CORS 失败
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 公共 GET：可匿名；若带 token 且有效则注入 UserContext
        if (isPublicGet(method, path)) {
            trySetUserContextFromToken(token,path);
            return true;
        }

        // 其他请求：必须登录
        if (trySetUserContextFromToken(token,path)) {
            return true;
        }

        log.warn("Token 无效或缺失，拦截请求：{} {}", method, path);
        writeUnauthorized(response);
        return false;
    }

    private boolean isPublicGet(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if (path.contains("/ws")) {
            return true;
        }
        // 公共 GET 路由：/goods, /goods/search, /goods/{goodsId}, /user/{userId}/home
        return path.endsWith("/goods")
                || path.endsWith("/goods/search")
                || path.matches(".*/goods/\\d+")
                || path.matches(".*/user/[^/]+/home");
    }
    private boolean trySetUserContextFromToken(String token, String path) {
        if (!hasText(token)) {
            return false;
        }
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        // 这里从 Redis 拿到的是 generateAdminToken 存入的 "ADMIN:admin01" 或者原有的 "2023001"
        String rawValue = tokenUtil.validateToken(cleanToken);

        if (rawValue == null) {
            return false;
        }
        rawValue = rawValue.replace("\"", "");
        boolean isAdminPath = path.contains("/admin"); // 判断是否是管理端接口
        boolean isAdminToken = rawValue.startsWith("ADMIN:"); // 判断是否是管理员 Token
        // 逻辑 A：访问管理端接口，但不是管理员 Token -> 拒绝
        if (isAdminPath && !isAdminToken) {
            log.warn("学生 Token 企图访问管理端接口: {}", path);
            return false;
        }
        // 逻辑 B：管理员 Token 访问学生端接口 -> 允许（方便测试），但要去掉前缀
        // 逻辑 C：正常的学生 Token 访问学生接口 -> 允许
        String finalId = isAdminToken ? rawValue.replace("ADMIN:", "") : rawValue;
        // 统一注入上下文，业务代码拿到的 ID 永远是不带前缀的纯 ID
        UserContext.setUserId(finalId);
        return true;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Result result = Result.fail(401, "未登录或登录已过期");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(result));
            writer.flush();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.remove();
    }
}
