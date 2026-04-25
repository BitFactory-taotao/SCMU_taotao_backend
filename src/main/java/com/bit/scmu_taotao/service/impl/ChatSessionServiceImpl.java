package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TTrade;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.mapper.ChatSessionMapper;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.RedisService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TTradeService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    private RedisService redisService;

    @Autowired
    private TTradeService tTradeService;

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
                targetUserMap.put("avatar", targetUser == null ? "" : targetUser.getAvatar());

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
    public Result initiateTrade(Long goodsId, String userId) {
        try {
            // 1) 基础校验阶段（DB）
            TGoods goods = tGoodsService.getById(goodsId);
            if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
                return Result.fail(404, "商品不存在");
            }
            if (!Integer.valueOf(GOODS_STATUS_ONLINE).equals(goods.getGoodsStatus())) {
                return Result.fail(400, "商品当前不可交易");
            }

            String sellerId = goods.getUserId();
            if (userId.equals(sellerId)) {
                return Result.fail(400, "不能与自己发起交易");
            }

            // 2) 意向状态检查（Redis）
            String tradeIntentKey = buildTradeIntentKey(goodsId, userId);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime;

            boolean hasIntent = Boolean.TRUE.equals(redisService.isExist(tradeIntentKey));
            if (hasIntent) {
                Long ttlSeconds = redisService.getExpire(tradeIntentKey, TimeUnit.SECONDS);
                if (ttlSeconds != null && ttlSeconds > 0) {
                    expireTime = now.plusSeconds(ttlSeconds);
                } else {
                    Map<String, Object> snapshot = buildTradeIntentSnapshot(goodsId, userId, sellerId, goods.getPrice(), now);
                    redisService.setWithExpire(tradeIntentKey, snapshot, TRADE_INTENT_EXPIRE_HOURS, TimeUnit.HOURS);
                    expireTime = now.plusHours(TRADE_INTENT_EXPIRE_HOURS);
                }
            } else {
                // 3) 数据写入（Redis）
                Map<String, Object> snapshot = buildTradeIntentSnapshot(goodsId, userId, sellerId, goods.getPrice(), now);
                redisService.setWithExpire(tradeIntentKey, snapshot, TRADE_INTENT_EXPIRE_HOURS, TimeUnit.HOURS);
                expireTime = now.plusHours(TRADE_INTENT_EXPIRE_HOURS);
            }

            // 4) 会话同步（DB）
            ChatSession session = findSessionByUsers(userId, sellerId);
            if (session == null) {
                session = new ChatSession();
                session.setUser1Id(userId);
                session.setUser2Id(sellerId);
                session.setStatus(1);
                session.setLastTime(new Date());
                this.save(session);
            } else if (!Integer.valueOf(1).equals(session.getStatus())) {
                session.setStatus(1);
                session.setLastTime(new Date());
                this.updateById(session);
            }

            appendTradeEventMessage(session.getChatId(), userId, sellerId, "买家已发起交易申请");

            // 5) 结果反馈
            Map<String, Object> data = new HashMap<>();
            data.put("expireTime", expireTime.format(TIME_FORMATTER));
            return Result.ok("交易发起成功，等待对方确认", data);
        } catch (Exception e) {
            log.error("发起交易失败: goodsId={}, userId={}", goodsId, userId, e);
            return Result.fail(500, "发起交易失败");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result withdrawTradeRequest(Long goodsId, String buyerId) {
        try {
            String tradeIntentKey = buildTradeIntentKey(goodsId, buyerId);
            if (!Boolean.TRUE.equals(redisService.isExist(tradeIntentKey))) {
                return Result.fail(400, "交易请求不存在或已失效");
            }
            TGoods goods = tGoodsService.getById(goodsId);
            if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
                return Result.fail(404, "商品不存在");
            }
            String sellerId = goods.getUserId();
            redisService.delete(tradeIntentKey);
            ChatSession session = findSessionByUsers(buyerId, sellerId);
            if (session != null) {
                appendTradeEventMessage(session.getChatId(), buyerId, sellerId, "买家已撤回交易申请");
            }
            log.info("买家撤回交易请求成功: goodsId={}, buyerId={}", goodsId, buyerId);
            return Result.ok("您已成功撤回交易请求", null);
        } catch (Exception e) {
            log.error("买家撤回交易请求失败: goodsId={}, buyerId={}", goodsId, buyerId, e);
            return Result.fail(500, "撤回交易请求失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result rejectTradeRequest(Long goodsId, String buyerId, String sellerId) {
        try {
            TGoods goods = tGoodsService.getById(goodsId);
            if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
                return Result.fail(404, "商品不存在");
            }
            if (!sellerId.equals(goods.getUserId())) {
                return Result.fail(403, "无权操作该交易请求");
            }

            String tradeIntentKey = buildTradeIntentKey(goodsId, buyerId);
            if (!Boolean.TRUE.equals(redisService.isExist(tradeIntentKey))) {
                return Result.fail(400, "交易请求不存在或已失效");
            }

            Map<String, Object> snapshot = getTradeIntentSnapshot(tradeIntentKey);
            String snapshotBuyerId = getMapString(snapshot, "buyerId");
            String snapshotSellerId = getMapString(snapshot, "sellerId");
            if (!sellerId.equals(snapshotSellerId) || !buyerId.equals(snapshotBuyerId)) {
                return Result.fail(400, "交易请求不存在或已失效");
            }

            redisService.delete(tradeIntentKey);
            ChatSession session = findSessionByUsers(buyerId, sellerId);
            if (session != null) {
                appendTradeEventMessage(session.getChatId(), sellerId, buyerId, "卖家已拒绝交易申请");
            }
            log.info("卖家拒绝交易请求成功: goodsId={}, buyerId={}, sellerId={}, intentKey={}", goodsId, buyerId, sellerId, tradeIntentKey);
            return Result.ok("已拒绝该交易请求", null);
        } catch (Exception e) {
            log.error("卖家拒绝交易请求失败: goodsId={}, buyerId={}, sellerId={}", goodsId, buyerId, sellerId, e);
            return Result.fail(500, "拒绝交易请求失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result confirmTrade(Long goodsId, String buyerId, String sellerId) {
        try {
            TGoods goods = tGoodsService.getById(goodsId);
            if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
                return Result.fail(404, "商品不存在");
            }
            if (!sellerId.equals(goods.getUserId())) {
                return Result.fail(403, "无权确认该交易");
            }
            if (!Integer.valueOf(GOODS_STATUS_ONLINE).equals(goods.getGoodsStatus())) {
                return Result.fail(400, "商品当前不可交易");
            }

            String tradeIntentKey = buildTradeIntentKey(goodsId, buyerId);
            if (!Boolean.TRUE.equals(redisService.isExist(tradeIntentKey))) {
                return Result.fail(400, "交易确认失败：请求已超时失效或买家已撤回意向");
            }

            Map<String, Object> snapshot = getTradeIntentSnapshot(tradeIntentKey);
            String snapshotBuyerId = getMapString(snapshot, "buyerId");
            String snapshotSellerId = getMapString(snapshot, "sellerId");
            if (!sellerId.equals(snapshotSellerId)
                    || snapshotBuyerId == null
                    || snapshotBuyerId.isBlank()
                    || !buyerId.equals(snapshotBuyerId)) {
                return Result.fail(400, "交易确认失败：请求已超时失效或买家已撤回意向");
            }

            BigDecimal tradePrice = parseTradePrice(snapshot.get("unitPrice"), goods.getPrice());
            LocalDateTime tradeTime = LocalDateTime.now();

            goods.setGoodsStatus(GOODS_STATUS_SOLD);
            if (!tGoodsService.updateById(goods)) {
                throw new RuntimeException("商品状态更新失败");
            }

            TTrade trade = new TTrade();
            trade.setGoodsId(goodsId);
            trade.setBuyerId(snapshotBuyerId);
            trade.setSellerId(sellerId);
            trade.setTradePrice(tradePrice);
            trade.setTradePlace(goods.getExchangePlace());
            trade.setTradeTime(tradeTime);
            trade.setIsDelete(0);
            if (!tTradeService.save(trade) || trade.getTradeId() == null) {
                throw new RuntimeException("交易记录创建失败");
            }

            redisService.delete(tradeIntentKey);

            ChatSession session = findSessionByUsers(buyerId, sellerId);
            if (session != null) {
                appendTradeEventMessage(session.getChatId(), sellerId, buyerId, "交易已确认，商品已成交");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("tradeId", trade.getTradeId());
            data.put("goodsId", goodsId);
            data.put("buyerId", snapshotBuyerId);
            data.put("sellerId", sellerId);
            data.put("tradePrice", tradePrice);
            data.put("tradeTime", tradeTime.format(TIME_FORMATTER));

            log.info("卖家确认交易成功: goodsId={}, tradeId={}, sellerId={}, buyerId={}", goodsId, trade.getTradeId(), sellerId, snapshotBuyerId);
            return Result.ok("成交成功！商品已下架，快联系买家线下交货吧", data);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("卖家确认交易失败: goodsId={}, buyerId={}, sellerId={}", goodsId, buyerId, sellerId, e);
            return Result.fail(500, "交易确认失败");
        }
    }

    private String buildTradeIntentKey(Long goodsId, String buyerId) {
        return TRADE_INTENT_PREFIX + goodsId + ":" + buyerId;
    }

    private Map<String, Object> buildTradeIntentSnapshot(Long goodsId, String buyerId, String sellerId, BigDecimal unitPrice, LocalDateTime now) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("goodsId", goodsId);
        snapshot.put("buyerId", buyerId);
        snapshot.put("sellerId", sellerId);
        snapshot.put("unitPrice", unitPrice);
        snapshot.put("requestTime", now.format(TIME_FORMATTER));
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getTradeIntentSnapshot(String key) {
        Object value = redisService.get(key);
        if (!(value instanceof Map)) {
            throw new IllegalStateException("交易意向快照不存在或格式异常");
        }
        return (Map<String, Object>) value;
    }


    private BigDecimal parseTradePrice(Object priceObj, BigDecimal fallbackPrice) {
        if (priceObj instanceof BigDecimal) {
            return (BigDecimal) priceObj;
        }
        if (priceObj instanceof Number) {
            return BigDecimal.valueOf(((Number) priceObj).doubleValue());
        }
        if (priceObj instanceof String && !((String) priceObj).isBlank()) {
            try {
                return new BigDecimal((String) priceObj);
            } catch (Exception ignored) {
                return fallbackPrice;
            }
        }
        return fallbackPrice;
    }

    private String getMapString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private void appendTradeEventMessage(Long chatId, String senderId, String receiverId, String content) {
        if (chatId == null || !StringUtils.hasText(senderId) || !StringUtils.hasText(receiverId) || !StringUtils.hasText(content)) {
            return;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatId(chatId);
        chatMessage.setSendId(senderId);
        chatMessage.setReceiveId(receiverId);
        chatMessage.setMsgType(1);
        chatMessage.setContentType("TRADE_EVENT");
        chatMessage.setMsgContent(content);
        chatMessage.setIsRead(0);
        chatMessage.setIsDelete(0);
        chatMessageMapper.insert(chatMessage);

        ChatSession updateChatSession = new ChatSession();
        updateChatSession.setChatId(chatId);
        updateChatSession.setLastMsg(content);
        updateChatSession.setLastTime(new Date());
        this.updateById(updateChatSession);
    }
}
