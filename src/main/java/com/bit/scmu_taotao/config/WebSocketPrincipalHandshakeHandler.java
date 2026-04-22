package com.bit.scmu_taotao.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * 将握手阶段解析出的 userId 绑定到 Principal，供 STOMP 用户队列使用。
 */
@Component
public class WebSocketPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userId = attributes.get("userId");
        if (userId == null) {
            return null;
        }
        String principalName = userId.toString();
        if (!StringUtils.hasText(principalName)) {
            return null;
        }
        return () -> principalName;
    }
}

