package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

/**
* @author 35314
* @description 针对表【chat_session(聊天会话表)】的数据库操作Service实现
* @createDate 2026-03-14 18:49:37
*/
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
    implements ChatSessionService{
    @Override
    public ChatSession findSessionByUsers(String userId1, String userId2) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        // 查询两个用户之间的会话，双向匹配
        queryWrapper.and(wrapper -> wrapper
                .eq(ChatSession::getUser1Id, userId1)
                .eq(ChatSession::getUser2Id, userId2)
        ).or(wrapper -> wrapper
                .eq(ChatSession::getUser1Id, userId2)
                .eq(ChatSession::getUser2Id, userId1)
        );
        return baseMapper.selectOne(queryWrapper);
    }
}




