package com.bit.scmu_taotao.controller;

import com.bit.scmu_taotao.dto.chat.StompChatSendRequest;
import com.bit.scmu_taotao.service.ChatMessageService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Slf4j
@Controller
@Validated
public class MessagesController {

    @Autowired
    private ChatMessageService chatMessageService;

    @MessageMapping("/messages/send")
    public void sendMessage(@Valid @Payload StompChatSendRequest request, Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new IllegalArgumentException("unauthorized websocket user");
        }

        String senderId = principal.getName();
        Result result = chatMessageService.sendByStomp(request.getChatId(), senderId, request.getReceiverId(), request.getContent());
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            throw new RuntimeException(result == null ? "failed to send chat message" : result.getMsg());
        }
    }
}
