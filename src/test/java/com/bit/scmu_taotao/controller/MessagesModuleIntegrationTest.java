package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TGoodsCategory;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.ChatMessageService;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.RedisService;
import com.bit.scmu_taotao.service.TGoodsCategoryService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessagesModuleIntegrationTest {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TUserService tUserService;
    @Autowired private TGoodsService tGoodsService;
    @Autowired private TGoodsCategoryService tGoodsCategoryService;
    @Autowired private ChatSessionService chatSessionService;
    @Autowired private ChatMessageService chatMessageService;
    @Autowired private RedisService redisService;
    @Autowired private TokenUtil tokenUtil;

    private String buyerId;
    private String sellerId;
    private String buyerToken;
    private Long goodsId;
    private Long chatId;
    private String tradeKey;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        buyerId = "msg_buyer_" + suffix;
        sellerId = "msg_seller_" + suffix;

        ensureUser(buyerId, "测试买家");
        ensureUser(sellerId, "测试卖家");
        Integer categoryId = ensureCategory("消息测试分类");
        goodsId = createGoods(sellerId, categoryId, 0);
        chatId = createChatSession(buyerId, sellerId);
        sendSellerMessages(chatId, sellerId);

        buyerToken = tokenUtil.generateToken(buyerId);
        tradeKey = "trade:req:" + goodsId + ":" + buyerId;
    }

    @AfterEach
    void tearDown() {
        if (chatId != null) {
            chatMessageService.remove(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getChatId, chatId));
            chatSessionService.removeById(chatId);
        }
        if (goodsId != null) {
            tGoodsService.removeById(goodsId);
        }
        tGoodsService.remove(new LambdaQueryWrapper<TGoods>().eq(TGoods::getUserId, buyerId).or().eq(TGoods::getUserId, sellerId));
        redisService.delete(tradeKey);
        if (buyerToken != null) {
            tokenUtil.invalidateToken(buyerToken);
        }
        tUserService.removeById(buyerId);
        tUserService.removeById(sellerId);
    }

    @Test
    @DisplayName("获取消息列表")
    void listMessages() throws Exception {
        Result result = callGet("/messages", Map.of("page", "1", "size", "10"));
        Map<String, Object> data = okData(result);
        List<Map<String, Object>> list = list(data);
        assertTrue(!list.isEmpty());
        assertEquals(chatId, asLong(list.get(0).get("chatId")));
        Map<String, Object> targetUser = (Map<String, Object>) list.get(0).get("targetUser");
        assertEquals(sellerId, targetUser.get("id"));
        assertEquals(2L, asLong(list.get(0).get("unreadCount")));
    }

    @Test
    @DisplayName("获取聊天消息详情")
    void listChatDetail() throws Exception {
        Result result = callGet("/messages/{chatId}", Map.of("page", "1", "size", "10"), chatId);
        Map<String, Object> data = okData(result);
        List<Map<String, Object>> list = list(data);
        assertEquals(2, list.size());
        assertTrue(((String) list.get(0).get("content")).startsWith("seller-msg-"));
    }

    @Test
    @DisplayName("发送聊天消息")
    void sendMessage() throws Exception {
        Result result = callPostJson("/messages/{chatId}", Map.of(), Map.of("content", "buyer-msg"), chatId);
        Map<String, Object> data = okData(result);
        assertNotNull(data.get("id"));
        assertNotNull(data.get("sendTime"));
    }

    @Test
    @DisplayName("标记单会话消息为已读")
    void markChatRead() throws Exception {
        Result result = callPut("/messages/{chatId}/read", Map.of(), chatId);
        assertEquals(200, result.getCode());
        assertEquals(0L, unreadCount());
    }

    @Test
    @DisplayName("清除所有未读消息")
    void clearUnreadMessages() throws Exception {
        Result result = callPut("/messages/unread/clear", Map.of());
        assertEquals(200, result.getCode());
        assertEquals(0L, unreadCount());
    }

    @Test
    @DisplayName("发起交易：自己给自己发起交易失败")
    void initiateTradeSelfFail() throws Exception {
        Long selfGoodsId = createGoods(buyerId, ensureCategory("消息测试分类"), 0);
        Result result = callPost("/messages/{goodsId}/trade", Map.of(), selfGoodsId);
        assertEquals(400, result.getCode());
        assertTrue(result.getMsg().contains("不能与自己发起交易"));
        tGoodsService.removeById(selfGoodsId);
    }

    @Test
    @DisplayName("发起交易：商品已成交/已下架失败")
    void initiateTradeGoodsNotAvailableFail() throws Exception {
        Long soldGoodsId = createGoods(sellerId, ensureCategory("消息测试分类"), 1);
        Result sold = callPost("/messages/{goodsId}/trade", Map.of(), soldGoodsId);
        assertEquals(400, sold.getCode());
        assertTrue(sold.getMsg().contains("商品当前不可交易"));
        tGoodsService.removeById(soldGoodsId);

        Long offlineGoodsId = createGoods(sellerId, ensureCategory("消息测试分类"), 2);
        Result offline = callPost("/messages/{goodsId}/trade", Map.of(), offlineGoodsId);
        assertEquals(400, offline.getCode());
        assertTrue(offline.getMsg().contains("商品当前不可交易"));
        tGoodsService.removeById(offlineGoodsId);
    }

    @Test
    @DisplayName("发起交易：Redis Key已存在时返回剩余TTL")
    void initiateTradeReuseRedisTtl() throws Exception {
        redisService.setWithExpire(tradeKey, new HashMap<>(Map.of("goodsId", goodsId)), 300, TimeUnit.SECONDS);
        LocalDateTime start = LocalDateTime.now();

        Result result = callPost("/messages/{goodsId}/trade", Map.of(), goodsId);
        Map<String, Object> data = okData(result);
        LocalDateTime expireTime = LocalDateTime.parse((String) data.get("expireTime"), TIME_FORMATTER);
        long remainSeconds = Duration.between(start, expireTime).getSeconds();

        assertEquals(200, result.getCode());
        assertTrue(remainSeconds >= 280 && remainSeconds <= 300);
    }

    private Result callGet(String path, Map<String, String> params, Object... uriVars) throws Exception {
        var request = uriVars.length == 0 ? get(path) : get(path, uriVars);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        request.header("Authorization", "Bearer " + buyerToken);
        return readResult(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private Result callPost(String path, Map<String, String> params, Object... uriVars) throws Exception {
        var request = uriVars.length == 0 ? post(path) : post(path, uriVars);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        request.header("Authorization", "Bearer " + buyerToken);
        return readResult(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private Result callPostJson(String path, Map<String, String> params, Object body, Object... uriVars) throws Exception {
        var request = uriVars.length == 0 ? post(path) : post(path, uriVars);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        request.header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        return readResult(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private Result callPut(String path, Map<String, String> params, Object... uriVars) throws Exception {
        var request = uriVars.length == 0 ? put(path) : put(path, uriVars);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        request.header("Authorization", "Bearer " + buyerToken);
        return readResult(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private Result readResult(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Result.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> okData(Result result) {
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        return objectMapper.convertValue(result.getData(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Map<String, Object> data) {
        Object value = data.get("list");
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }

    private long unreadCount() throws Exception {
        Result result = callGet("/messages", Map.of("page", "1", "size", "10"));
        List<Map<String, Object>> sessions = list(okData(result));
        return sessions.isEmpty() ? 0L : asLong(sessions.get(0).get("unreadCount"));
    }

    private void ensureUser(String userId, String userName) {
        if (tUserService.getById(userId) != null) {
            return;
        }
        TUser user = new TUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setCreditScore(100);
        user.setCreditStar(new BigDecimal("5.0"));
        user.setIsDelete(0);
        tUserService.save(user);
    }

    private Integer ensureCategory(String categoryName) {
        TGoodsCategory category = tGoodsCategoryService.getOne(new LambdaQueryWrapper<TGoodsCategory>()
                .eq(TGoodsCategory::getCategoryName, categoryName)
                .last("LIMIT 1"));
        if (category != null) {
            return category.getCategoryId();
        }
        TGoodsCategory newCategory = new TGoodsCategory();
        newCategory.setCategoryName(categoryName);
        newCategory.setSort(1);
        newCategory.setIsShow(0);
        tGoodsCategoryService.save(newCategory);
        return newCategory.getCategoryId();
    }

    private Long createGoods(String ownerId, Integer categoryId, Integer status) {
        TGoods goods = new TGoods();
        goods.setUserId(ownerId);
        goods.setCategoryId(categoryId);
        goods.setGoodsType(1);
        goods.setGoodsName("消息测试商品");
        goods.setGoodsDesc("消息测试描述");
        goods.setGoodsNote("消息测试备注");
        goods.setPrice(new BigDecimal("9.90"));
        goods.setUseScene("测试场景");
        goods.setExchangePlace("测试地点");
        goods.setGoodsStatus(status);
        goods.setIsDelete(0);
        tGoodsService.save(goods);
        return goods.getGoodsId();
    }

    private Long createChatSession(String buyerId, String sellerId) {
        ChatSession session = new ChatSession();
        session.setUser1Id(buyerId);
        session.setUser2Id(sellerId);
        session.setStatus(1);
        session.setLastMsg("初始化消息");
        session.setLastTime(new Date());
        session.setCreatedAt(LocalDateTime.now());
        chatSessionService.save(session);
        return session.getChatId();
    }

    private void sendSellerMessages(Long chatId, String sellerId) {
        chatMessageService.sendByChatId(chatId, sellerId, "seller-msg-1");
        chatMessageService.sendByChatId(chatId, sellerId, "seller-msg-2");
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }
}

