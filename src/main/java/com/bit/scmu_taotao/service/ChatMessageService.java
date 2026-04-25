package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.chat.MessageSendMediaRequest;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.util.common.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 35314
* @description 针对表【chat_message(沟通消息详情表)】的数据库操作Service
* @createDate 2026-03-14 18:49:37
*/
public interface ChatMessageService extends IService<ChatMessage> {

	Result listByChatId(Long chatId, String userId, Integer page, Integer size);

	Result sendByChatId(Long chatId, String senderId, String content);

	Result sendByStomp(Long chatId, String senderId, String receiverId, String content);

	Result sendMediaByChatId(Long chatId, String senderId, MessageSendMediaRequest request);

	Result sendMediaByStomp(Long chatId, String senderId, String receiverId, MessageSendMediaRequest request);

	Result clearUnreadByUserId(String userId);

	Result markChatRead(Long chatId, String userId);


}
