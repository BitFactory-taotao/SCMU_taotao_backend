package com.bit.scmu_taotao.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息模块 WebSocket 处理器
 */
@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        if (!StringUtils.hasText(userId)) {
            log.warn("WebSocket 连接建立失败：缺少 userId");
            tryClose(session, CloseStatus.NOT_ACCEPTABLE.withReason("invalid user"));
            return;
        }
        userSessionMap.put(userId, session);
        log.info("WebSocket 连接成功：userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // TODO 当前阶段仅保留连接能力，消息收发在消息模块服务接入后实现
        log.debug("收到 WebSocket 文本消息：userId={}, payload={}", getUserId(session), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        if (StringUtils.hasText(userId)) {
            userSessionMap.remove(userId);
        }
        log.info("WebSocket 连接关闭：userId={}, status={}", userId, status);
    }

    private String getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId == null ? null : userId.toString();
    }

    private void tryClose(WebSocketSession session, CloseStatus closeStatus) {
        try {
            session.close(closeStatus);
        } catch (Exception e) {
            log.warn("关闭非法 WebSocket 连接失败", e);
        }
    }
}

