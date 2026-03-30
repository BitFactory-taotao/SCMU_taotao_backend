package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.entity.ChatSession;
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
}
