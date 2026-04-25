package com.bit.scmu_taotao.interceptor;

import com.bit.scmu_taotao.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器
 */
@Slf4j
@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private TokenUtil tokenUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("WebSocket 握手失败：缺少 token");
            return false;
        }

        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String userId = tokenUtil.validateToken(cleanToken);
        if (!StringUtils.hasText(userId)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("WebSocket 握手失败：token 无效");
            return false;
        }

        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        /*List<String> authorizationHeaders = request.getHeaders().get("Authorization");
        if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
            return authorizationHeaders.get(0);
        }

        String query = request.getURI().getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;*/
        // 1. 优先从 Header 获取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader)) {
            // 如果有 Bearer 前缀，去掉它
            return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        }

        // 2. 备选方案：从 URL 查询参数获取 (用于解决 WebSocket 握手无法带 Header 的限制)
        // 使用 Spring 的 UriComponentsBuilder 优雅地解析参数
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();

        String token = queryParams.getFirst("token");
        if (StringUtils.hasText(token)) {
            // 同样处理参数中可能带有的 Bearer 前缀（虽然参数中通常不带，但为了鲁棒性建议加上）
            return token.startsWith("Bearer ") ? token.substring(7) : token;
        }

        return null;
    }
}

