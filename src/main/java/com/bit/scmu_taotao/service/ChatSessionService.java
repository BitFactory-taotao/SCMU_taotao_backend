package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.util.common.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 35314
* @description 针对表【chat_session(聊天会话表)】的数据库操作Service
* @createDate 2026-03-14 18:49:37
*/
public interface ChatSessionService extends IService<ChatSession> {
    /**
     * 根据两个用户ID查询会话
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 聊天会话
     */
    ChatSession findSessionByUsers(String userId1, String userId2);

    Result listMySessions(String userId, Integer page, Integer size);

    Result initiateTrade(Long goodsId, String userId);

    Result contactSeller(Long goodsId, String userId);

    Result withdrawTradeRequest(Long goodsId, String buyerId);

    Result rejectTradeRequest(Long goodsId, String buyerId, String sellerId);

    Result confirmTrade(Long goodsId, String buyerId, String sellerId);
}
