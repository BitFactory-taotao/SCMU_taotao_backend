package com.bit.scmu_taotao.controller;

import com.bit.scmu_taotao.dto.chat.MessageSendRequest;
import com.bit.scmu_taotao.service.ChatMessageService;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/messages")
public class MessagesRestController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageService chatMessageService;

    @GetMapping
    public Result listMessages(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                               @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatSessionService.listMySessions(userId, page, size);
    }

    @GetMapping("/{chatId}")
    public Result listChatDetail(@PathVariable("chatId") @Min(1) Long chatId,
                                 @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                 @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatMessageService.listByChatId(chatId, userId, page, size);
    }

    @PostMapping("/{chatId}")
    public Result sendMessage(@PathVariable("chatId") @Min(1) Long chatId,
                              @Valid @RequestBody MessageSendRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatMessageService.sendByChatId(chatId, userId, request.getContent());
    }

    @PostMapping("/{goodsId}/trade")
    public Result initiateTrade(@PathVariable("goodsId") @Min(1) Long goodsId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatSessionService.initiateTrade(goodsId, userId);
    }

    @PostMapping("/goods/{goodsId}/contact")
    public Result contactSeller(@PathVariable("goodsId") @Min(1) Long goodsId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatSessionService.contactSeller(goodsId, userId);
    }

    @PutMapping("/unread/clear")
    public Result clearUnreadMessages() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatMessageService.clearUnreadByUserId(userId);
    }

    @PutMapping("/{chatId}/read")
    public Result markChatRead(@PathVariable("chatId") @Min(1) Long chatId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatMessageService.markChatRead(chatId, userId);
    }
}


