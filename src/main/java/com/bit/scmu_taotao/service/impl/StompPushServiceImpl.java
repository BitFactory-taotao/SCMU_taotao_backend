package com.bit.scmu_taotao.service.impl;

import com.bit.scmu_taotao.service.StompPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class StompPushServiceImpl implements StompPushService {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void pushToUserQueue(String userId, String destination, Object payload) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(destination)) {
            log.warn("STOMP push skipped: invalid userId or destination, userId={}, destination={}", userId, destination);
            return;
        }
        simpMessagingTemplate.convertAndSendToUser(userId, destination, payload);
    }
}

