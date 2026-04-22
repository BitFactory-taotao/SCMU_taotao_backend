package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TGoodsCategory;
import com.bit.scmu_taotao.entity.TTrade;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.RedisService;
import com.bit.scmu_taotao.service.TGoodsCategoryService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TTradeService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessagesTradeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private TTradeService tTradeService;

    private String buyerId;
    private String sellerId;
    private String buyerToken;
    private String sellerToken;
    private Long goodsId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        buyerId = "trade_buyer_" + suffix;
        sellerId = "trade_seller_" + suffix;

        ensureUser(buyerId, "测试买家");
        ensureUser(sellerId, "测试卖家");

        Integer categoryId = ensureCategory("交易测试分类");
        goodsId = createGoods(sellerId, categoryId);
        ensureChatSession(buyerId, sellerId);
        buyerToken = tokenUtil.generateToken(buyerId);
        sellerToken = tokenUtil.generateToken(sellerId);
    }

    @AfterEach
    void tearDown() {
        if (goodsId != null) {
            redisService.delete("trade:req:" + goodsId + ":" + buyerId);
            tTradeService.remove(new LambdaQueryWrapper<TTrade>().eq(TTrade::getGoodsId, goodsId));
            tGoodsService.removeById(goodsId);
        }

        chatSessionService.remove(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUser1Id, buyerId)
                .eq(ChatSession::getUser2Id, sellerId)
                .or(w -> w.eq(ChatSession::getUser1Id, sellerId).eq(ChatSession::getUser2Id, buyerId)));

        redisService.delete(buyerToken);
        redisService.delete("token_" + buyerId);
        redisService.delete(sellerToken);
        redisService.delete("token_" + sellerId);

        tUserService.removeById(buyerId);
        tUserService.removeById(sellerId);
    }

    @Test
    @DisplayName("发起交易成功：返回200和expireTime")
    void initiateTradeSuccess() throws Exception {
        Result result = callTrade(goodsId, true);
        assertEquals(200, result.getCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data);
        assertTrue(data.containsKey("expireTime"));
        assertNotNull(data.get("expireTime"));
    }

    @Test
    @DisplayName("未登录发起交易：返回401")
    void initiateTradeUnauthorized() throws Exception {
        Result result = callTrade(goodsId, false);
        assertEquals(401, result.getCode());
    }

    @Test
    @DisplayName("商品不存在：返回404")
    void initiateTradeGoodsNotFound() throws Exception {
        Result result = callTrade(99999999L, true);
        assertEquals(404, result.getCode());
    }

    @Test
    @DisplayName("买家撤回交易成功：返回200并删除交易意向")
    void withdrawTradeRequestSuccess() throws Exception {
        callTrade(goodsId, true);
        Result result = callTradeWithdraw(goodsId, buyerToken, true);
        assertEquals(200, result.getCode());
        assertEquals("您已成功撤回交易请求", result.getMsg());
        assertFalse(Boolean.TRUE.equals(redisService.isExist("trade:req:" + goodsId + ":" + buyerId)));
    }

    @Test
    @DisplayName("卖家拒绝交易成功：返回200并删除交易意向")
    void rejectTradeRequestSuccess() throws Exception {
        callTrade(goodsId, true);
        Result result = callTradeReject(goodsId, buyerId, sellerToken, true);
        assertEquals(200, result.getCode());
        assertEquals("已拒绝该交易请求", result.getMsg());
        assertFalse(Boolean.TRUE.equals(redisService.isExist("trade:req:" + goodsId + ":" + buyerId)));
    }

    @Test
    @DisplayName("卖家确认交易成功：返回200并写入交易记录")
    void confirmTradeSuccess() throws Exception {
        callTrade(goodsId, true);
        Result result = callTradeConfirm(goodsId, buyerId, sellerToken, true);
        assertEquals(200, result.getCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data);
        assertNotNull(data.get("tradeId"));
        assertEquals(goodsId.longValue(), ((Number) data.get("goodsId")).longValue());

        TGoods goods = tGoodsService.getById(goodsId);
        assertNotNull(goods);
        assertEquals(1, goods.getGoodsStatus());

        TTrade trade = tTradeService.getOne(new LambdaQueryWrapper<TTrade>()
                .eq(TTrade::getGoodsId, goodsId)
                .eq(TTrade::getBuyerId, buyerId)
                .eq(TTrade::getSellerId, sellerId)
                .eq(TTrade::getIsDelete, 0)
                .last("limit 1"), false);
        assertNotNull(trade);
        assertNotNull(trade.getTradeId());
    }

    @Test
    @DisplayName("卖家确认交易失败：意向已失效")
    void confirmTradeMissingIntent() throws Exception {
        Result result = callTradeConfirm(goodsId, buyerId, sellerToken, true);
        assertEquals(400, result.getCode());
        assertEquals("交易确认失败：请求已超时失效或买家已撤回意向", result.getMsg());
    }

    private Result callTrade(Long gid, boolean withToken) throws Exception {
        var request = post("/messages/{goodsId}/trade", gid);
        if (withToken) {
            request.header("Authorization", "Bearer " + buyerToken);
        }

        MvcResult mvcResult = withToken
                ? mockMvc.perform(request).andExpect(status().isOk()).andReturn()
                : mockMvc.perform(request).andExpect(status().isUnauthorized()).andReturn();

        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Result.class);
    }

    private Result callTradeWithdraw(Long gid, String token, boolean withToken) throws Exception {
        var request = delete("/trade/request/{goodsId}", gid);
        if (withToken) {
            request.header("Authorization", "Bearer " + token);
        }

        MvcResult mvcResult = withToken
                ? mockMvc.perform(request).andExpect(status().isOk()).andReturn()
                : mockMvc.perform(request).andExpect(status().isUnauthorized()).andReturn();

        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Result.class);
    }

    private Result callTradeReject(Long gid, String buyerId, String token, boolean withToken) throws Exception {
        var request = post("/trade/reject/{goodsId}/{buyerId}", gid, buyerId);
        if (withToken) {
            request.header("Authorization", "Bearer " + token);
        }

        MvcResult mvcResult = withToken
                ? mockMvc.perform(request).andExpect(status().isOk()).andReturn()
                : mockMvc.perform(request).andExpect(status().isUnauthorized()).andReturn();

        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Result.class);
    }

    private Result callTradeConfirm(Long gid, String buyerId, String token, boolean withToken) throws Exception {
        var request = post("/trade/confirm/{goodsId}/{buyerId}", gid, buyerId);
        if (withToken) {
            request.header("Authorization", "Bearer " + token);
        }

        MvcResult mvcResult = withToken
                ? mockMvc.perform(request).andExpect(status().isOk()).andReturn()
                : mockMvc.perform(request).andExpect(status().isUnauthorized()).andReturn();

        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Result.class);
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

    private Long createGoods(String ownerId, Integer categoryId) {
        TGoods goods = new TGoods();
        goods.setUserId(ownerId);
        goods.setCategoryId(categoryId);
        goods.setGoodsType(1);
        goods.setGoodsName("交易测试商品");
        goods.setGoodsDesc("交易测试描述");
        goods.setGoodsNote("交易测试备注");
        goods.setPrice(new BigDecimal("9.90"));
        goods.setUseScene("测试场景");
        goods.setExchangePlace("测试地点");
        goods.setGoodsStatus(0);
        goods.setIsDelete(0);
        tGoodsService.save(goods);
        return goods.getGoodsId();
    }

    private void ensureChatSession(String user1Id, String user2Id) {
        ChatSession existing = chatSessionService.getOne(new LambdaQueryWrapper<ChatSession>()
                .and(w -> w.eq(ChatSession::getUser1Id, user1Id).eq(ChatSession::getUser2Id, user2Id))
                .or(w -> w.eq(ChatSession::getUser1Id, user2Id).eq(ChatSession::getUser2Id, user1Id))
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }

        ChatSession session = new ChatSession();
        session.setUser1Id(user1Id);
        session.setUser2Id(user2Id);
        session.setStatus(1);
        session.setLastTime(new java.util.Date());
        session.setCreatedAt(LocalDateTime.now());
        chatSessionService.save(session);
    }
}

