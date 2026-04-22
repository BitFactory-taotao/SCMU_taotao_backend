package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.chat.StompChatMessageDTO;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.mapper.ChatSessionMapper;
import com.bit.scmu_taotao.service.ChatMessageService;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.service.StompPushService;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author 35314
* @description 针对表【chat_message(沟通消息详情表)】的数据库操作Service实现
* @createDate 2026-03-14 18:49:37
*/
@Service
@Slf4j
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private StompPushService stompPushService;

    @Override
    public Result listByChatId(Long chatId, String userId, Integer page, Integer size) {
        try {
            ChatSession session = chatSessionMapper.selectById(chatId);
            if (session == null) {
                return Result.fail(404, "会话不存在");
            }

            boolean isParticipant = userId.equals(session.getUser1Id()) || userId.equals(session.getUser2Id());
            if (!isParticipant) {
                return Result.fail(403, "无权访问该会话");
            }

            Page<ChatMessage> queryPage = new Page<>(page, size);
            LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChatMessage::getChatId, chatId)
                    .eq(ChatMessage::getIsDelete, 0)
                    .orderByDesc(ChatMessage::getCreateTime);

            Page<ChatMessage> resultPage = this.page(queryPage, queryWrapper);
            List<Map<String, Object>> list = new ArrayList<>();
            for (ChatMessage message : resultPage.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", message.getMsgId());
                item.put("content", message.getMsgContent());
                item.put("sendTime", message.getCreateTime() == null ? null : message.getCreateTime().format(TIME_FORMATTER));
                item.put("senderId", message.getSendId());
                item.put("receiverId", message.getReceiveId());
                item.put("isRead", Integer.valueOf(1).equals(message.getIsRead()));
                list.add(item);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("查询聊天详情失败: chatId={}, userId={}, page={}, size={}", chatId, userId, page, size, e);
            return Result.fail(500, "查询聊天详情失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sendByChatId(Long chatId, String senderId, String content) {
        return doSend(chatId, senderId, null, content, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sendByStomp(Long chatId, String senderId, String receiverId, String content) {
        return doSend(chatId, senderId, receiverId, content, false);
    }

    private Result doSend(Long chatId, String senderId, String receiverId, String content, boolean updateSession) {
        try {
            if (!StringUtils.hasText(content)) {
                return Result.fail(400, "消息内容不能为空");
            }

            ChatSession session = chatSessionMapper.selectById(chatId);
            if (session == null) {
                return Result.fail(404, "会话不存在");
            }

            boolean isParticipant = senderId.equals(session.getUser1Id()) || senderId.equals(session.getUser2Id());
            if (!isParticipant) {
                return Result.fail(403, "无权发送该会话消息");
            }

            String actualReceiverId = senderId.equals(session.getUser1Id()) ? session.getUser2Id() : session.getUser1Id();
            if (StringUtils.hasText(receiverId) && !receiverId.equals(actualReceiverId)) {
                return Result.fail(400, "接收方与会话不匹配");
            }

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setChatId(chatId);
            chatMessage.setSendId(senderId);
            chatMessage.setReceiveId(actualReceiverId);
            chatMessage.setMsgType(2);
            chatMessage.setMsgContent(content);
            chatMessage.setIsRead(0);
            chatMessage.setIsDelete(0);
            boolean saved = this.save(chatMessage);
            if (!saved) {
                throw new RuntimeException("保存消息失败");
            }

            if (updateSession) {
                ChatSession updateChatSession = new ChatSession();
                updateChatSession.setChatId(chatId);
                updateChatSession.setLastMsg(content);
                updateChatSession.setLastTime(new Date());
                int sessionUpdateRows = chatSessionMapper.updateById(updateChatSession);
                if (sessionUpdateRows <= 0) {
                    throw new RuntimeException("更新会话失败");
                }
            }

            StompChatMessageDTO messageDTO = new StompChatMessageDTO();
            messageDTO.setId(chatMessage.getMsgId());
            messageDTO.setChatId(chatMessage.getChatId());
            messageDTO.setSenderId(chatMessage.getSendId());
            messageDTO.setReceiverId(chatMessage.getReceiveId());
            messageDTO.setContent(chatMessage.getMsgContent());
            messageDTO.setSendTime(chatMessage.getCreateTime() == null ? LocalDateTime.now() : chatMessage.getCreateTime());
            messageDTO.setRead(false);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        stompPushService.pushToUserQueue(actualReceiverId, "/queue/messages", messageDTO);
                        stompPushService.pushToUserQueue(senderId, "/queue/messages/ack", messageDTO);
                        log.info("消息发送成功并推送: chatId={}, senderId={}, receiverId={}, msgId={}",
                                chatId, senderId, actualReceiverId, chatMessage.getMsgId());
                    } catch (Exception ex) {
                        log.error("消息发送后推送失败: chatId={}, senderId={}, receiverId={}, msgId={}",
                                chatId, senderId, actualReceiverId, chatMessage.getMsgId(), ex);
                    }
                }
            });

            Map<String, Object> data = new HashMap<>();
            data.put("id", chatMessage.getMsgId());
            data.put("sendTime", messageDTO.getSendTime().format(TIME_FORMATTER));
            return Result.ok("发送成功", data);
        } catch (Exception e) {
            log.error("发送消息失败: chatId={}, senderId={}", chatId, senderId, e);
            return Result.fail(500, "发送消息失败");
        }
    }

    @Override
    public Result clearUnreadByUserId(String userId) {
        try {
            boolean updated = this.update(
                    new LambdaUpdateWrapper<ChatMessage>()
                            .set(ChatMessage::getIsRead, 1) // 直接指定更新字段
                            .eq(ChatMessage::getReceiveId, userId)
                            .eq(ChatMessage::getIsRead, 0)
                            .eq(ChatMessage::getIsDelete, 0)
            );
            if (!updated) {
                log.info("清除未读消息完成(无可更新数据): userId={}", userId);
            }
            return Result.ok("未读消息已全部清除", null);
        } catch (Exception e) {
            log.error("清除全部未读消息失败: userId={}", userId, e);
            return Result.fail(500, "清除未读消息失败");
        }
    }

    @Override
    public Result markChatRead(Long chatId, String userId) {
        try {
            ChatSession session = chatSessionMapper.selectById(chatId);
            if (session == null) {
                return Result.fail(404, "会话不存在");
            }

            boolean isParticipant = userId.equals(session.getUser1Id()) || userId.equals(session.getUser2Id());
            if (!isParticipant) {
                return Result.fail(403, "无权操作该会话");
            }

            boolean updated = this.update(
                    new LambdaUpdateWrapper<ChatMessage>()
                            .set(ChatMessage::getIsRead, 1) // 直接指定更新字段
                            .eq(ChatMessage::getChatId, chatId)
                            .eq(ChatMessage::getReceiveId, userId)
                            .eq(ChatMessage::getIsRead, 0)
                            .eq(ChatMessage::getIsDelete, 0)
            );
            if (!updated) {
                log.info("会话已读标记完成(无可更新数据): chatId={}, userId={}", chatId, userId);
            }
            return Result.ok("消息已标记为已读", null);
        } catch (Exception e) {
            log.error("标记会话已读失败: chatId={}, userId={}", chatId, userId, e);
            return Result.fail(500, "标记已读失败");
        }
    }

}




