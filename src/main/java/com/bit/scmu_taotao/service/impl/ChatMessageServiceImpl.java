package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.service.ChatMessageService;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author 35314
* @description 针对表【chat_message(沟通消息详情表)】的数据库操作Service实现
* @createDate 2026-03-14 18:49:37
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{

}




