package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.mapper.ChatSessionMapper;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.bit.scmu_taotao.util.common.KeyDescription.*;

/**
 * @author 35314
 * @description 针对表【chat_session(聊天会话表)】的数据库操作Service实现
 * @createDate 2026-03-14 18:49:37
 */
@Service
@Slf4j
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TGoodsService tGoodsService;

    @Override
    public ChatSession findSessionByUsers(String userId1, String userId2) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq(ChatSession::getUser1Id, userId1)
                .eq(ChatSession::getUser2Id, userId2)
        ).or(wrapper -> wrapper
                .eq(ChatSession::getUser1Id, userId2)
                .eq(ChatSession::getUser2Id, userId1)
        );
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public Result listMySessions(String userId, Integer page, Integer size) {
        try {
            Page<ChatSession> queryPage = new Page<>(page, size);
            LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.and(wrapper -> wrapper.eq(ChatSession::getUser1Id, userId).or().eq(ChatSession::getUser2Id, userId))
                    .ne(ChatSession::getStatus, 3)
                    .orderByDesc(ChatSession::getLastTime);

            Page<ChatSession> resultPage = this.page(queryPage, queryWrapper);
            List<ChatSession> sessions = resultPage.getRecords();
            if (sessions.isEmpty()) {
                return Result.ok("请求成功", Collections.emptyMap());
            }

            // 1. 提取所有目标用户ID并批量查询用户信息
            Set<String> targetUserIds = sessions.stream()
                    .map(s -> userId.equals(s.getUser1Id()) ? s.getUser2Id() : s.getUser1Id())
                    .collect(Collectors.toSet());

            Map<String, TUser> userMap = tUserService.listByIds(targetUserIds).stream()
                    .collect(Collectors.toMap(TUser::getUserId, u -> u));

            // 2. 批量查询未读消息数 (使用 GROUP BY)
            List<Long> chatIds = sessions.stream().map(ChatSession::getChatId).collect(Collectors.toList());
            QueryWrapper<ChatMessage> unreadWrapper = new QueryWrapper<>();
            unreadWrapper.select("chat_id", "count(*) as count")
                    .in("chat_id", chatIds)
                    .eq("receive_id", userId)
                    .eq("is_read", 0)
                    .eq("is_delete", 0)
                    .groupBy("chat_id");

            // 将未读数转为 Map<chatId, count>
            List<Map<String, Object>> unreadMaps = chatMessageMapper.selectMaps(unreadWrapper);
            Map<Long, Long> unreadCountMap = unreadMaps.stream().collect(Collectors.toMap(
                    m -> ((Number) m.get("chat_id")).longValue(),
                    m -> ((Number) m.get("count")).longValue()
            ));

            // 3. 组装数据
            List<Map<String, Object>> list = sessions.stream().map(session -> {
                String targetUserId = userId.equals(session.getUser1Id()) ? session.getUser2Id() : session.getUser1Id();
                TUser targetUser = userMap.get(targetUserId);

                Map<String, Object> targetUserMap = new HashMap<>();
                targetUserMap.put("id", targetUserId);
                targetUserMap.put("name", targetUser == null ? "" : targetUser.getUserName());
                targetUserMap.put("avatar", targetUser == null ? DEFAULT_AVATAR : targetUser.getAvatar());

                Map<String, Object> item = new HashMap<>();
                item.put("chatId", session.getChatId());
                item.put("targetUser", targetUserMap);
                item.put("lastContent", session.getLastMsg());
                item.put("lastTime", session.getLastTime() == null ? null :
                        session.getLastTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(TIME_FORMATTER));
                item.put("unreadCount", unreadCountMap.getOrDefault(session.getChatId(), 0L));
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);
            return Result.ok("请求成功", data);

        } catch (Exception e) {
            log.error("查询会话列表失败", e);
            return Result.fail(500, "系统繁忙");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result contactSeller(Long goodsId, String userId) {
        try {
            TGoods goods = tGoodsService.getById(goodsId);
            if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
                return Result.fail(404, "商品不存在");
            }
            if (!Integer.valueOf(GOODS_STATUS_ONLINE).equals(goods.getGoodsStatus())) {
                return Result.fail(400, "商品当前不可联系");
            }

            String sellerId = goods.getUserId();
            if (sellerId == null || sellerId.isBlank()) {
                return Result.fail(400, "商品未绑定商家");
            }
            if (userId.equals(sellerId)) {
                return Result.fail(400, "不能联系自己");
            }

            ChatSession session = findSessionByUsers(userId, sellerId);
            boolean created = false;
            Date now = new Date();

            if (session == null) {
                session = new ChatSession();
                session.setUser1Id(userId);
                session.setUser2Id(sellerId);
                session.setStatus(1);
                session.setLastTime(now);
                this.save(session);
                created = true;
                appendContactInitSystemMessages(session.getChatId(), userId, sellerId);
            } else {
                if (!Integer.valueOf(1).equals(session.getStatus())) {
                    session.setStatus(1);
                }
                session.setLastTime(now);
                this.updateById(session);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("chatId", session.getChatId());
            data.put("sellerId", sellerId);
            data.put("created", created);
            return Result.ok("联系商家成功", data);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("联系商家失败: goodsId={}, userId={}", goodsId, userId, e);
            return Result.fail(500, "联系商家失败");
        }
    }

    private void appendContactInitSystemMessages(Long chatId, String buyerId, String sellerId) {
        if (chatId == null || !StringUtils.hasText(buyerId) || !StringUtils.hasText(sellerId)) {
            return;
        }

        insertSystemMessage(chatId, "system", buyerId, 0, null, "您已开始与商家沟通，请注意文明交流");
        insertSystemMessage(chatId, "system", sellerId, 0, null, "有用户对您的商品感兴趣，开始了沟通");
    }

    /**
     * 插入一条系统/交易事件消息
     */
    private void insertSystemMessage(Long chatId, String sendId, String receiveId, Integer msgType, String contentType, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatId(chatId);
        chatMessage.setSendId(sendId);
        chatMessage.setReceiveId(receiveId);
        chatMessage.setMsgType(msgType);
        chatMessage.setContentType(contentType);
        chatMessage.setMsgContent(content);
        chatMessage.setIsRead(0);
        chatMessage.setIsDelete(0);
        chatMessageMapper.insert(chatMessage);
    }
}
