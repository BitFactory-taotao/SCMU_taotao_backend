package com.bit.scmu_taotao.controller;

import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
public class TradeController {

    @Autowired
    private ChatSessionService chatSessionService;

    @DeleteMapping("/trade/request/{goodsId}")
    public Result withdrawTradeRequest(@PathVariable("goodsId") @Min(1) Long goodsId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        log.info("收到买家撤回交易请求: goodsId={}, buyerId={}", goodsId, userId);
        return chatSessionService.withdrawTradeRequest(goodsId, userId);
    }

    @PostMapping("/trade/reject/{goodsId}/{buyerId}")
    public Result rejectTradeRequest(@PathVariable("goodsId") @Min(1) Long goodsId,
                                     @PathVariable("buyerId") String buyerId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        log.info("收到卖家拒绝交易请求: goodsId={}, buyerId={}, sellerId={}", goodsId, buyerId, userId);
        return chatSessionService.rejectTradeRequest(goodsId, buyerId, userId);
    }

    @PostMapping("/trade/confirm/{goodsId}/{buyerId}")
    public Result confirmTrade(@PathVariable("goodsId") @Min(1) Long goodsId,
                               @PathVariable("buyerId") String buyerId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        log.info("收到卖家确认交易请求: goodsId={}, buyerId={}, sellerId={}", goodsId, buyerId, userId);
        return chatSessionService.confirmTrade(goodsId, buyerId, userId);
    }
}

