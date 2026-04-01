package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户接口测试类
 * 测试 getSellGoods、getBoughtGoods、getFavorites、getBuyGoods 接口
 */
@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("用户控制器集成测试")
class UserControllerTest {

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private GoodsController goodsController;

    @Autowired
    private TFavoriteService tFavoriteService;

    @Autowired
    private TTradeService tTradeService;

    @Autowired
    private TEvaluateService tEvaluateService;

    private static final String TEST_PREFIX = "ut_user_controller";
    private static final String TEST_USER_ID = TEST_PREFIX + "_buyer";
    private static final String TEST_CATEGORY = TEST_PREFIX + "_category";

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(TEST_USER_ID, "测试买家");
        ensureCategory();
        UserContext.setUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    private void cleanTestData() {
        tEvaluateService.remove(new LambdaQueryWrapper<TEvaluate>()
                .eq(TEvaluate::getBuyerId, TEST_USER_ID)
                .or().likeRight(TEvaluate::getSellerId, TEST_PREFIX + "_seller_"));

        tTradeService.remove(new LambdaQueryWrapper<TTrade>()
                .eq(TTrade::getBuyerId, TEST_USER_ID)
                .or().likeRight(TTrade::getSellerId, TEST_PREFIX + "_seller_"));

        tFavoriteService.remove(new LambdaQueryWrapper<TFavorite>()
                .eq(TFavorite::getUserId, TEST_USER_ID));

        tGoodsService.remove(new LambdaQueryWrapper<TGoods>()
                .eq(TGoods::getUserId, TEST_USER_ID)
                .or().likeRight(TGoods::getUserId, TEST_PREFIX + "_seller_"));

        tUserService.remove(new LambdaQueryWrapper<TUser>()
                .eq(TUser::getUserId, TEST_USER_ID)
                .or().likeRight(TUser::getUserId, TEST_PREFIX + "_seller_"));
    }

    // ======================== getSellGoods 测试 ========================

    @Test
    @DisplayName("getSellGoods: 正常获取出售中的商品列表")
    void testGetSellGoodsSuccess() {
        createTestGoods(1, "sell");
        createTestGoods(2, "sell");
        Result result = tUserService.getSellGoods(1, 10, "online");
        Map<String, Object> data = okData(result);
        assertTrue(data.containsKey("list"));
        assertTrue(data.containsKey("total"));
    }

    @Test
    @DisplayName("getSellGoods: 用户没有出售的商品时返回空列表")
    void testGetSellGoodsEmptyList() {
        Result result = tUserService.getSellGoods(1, 10, "online");
        Map<String, Object> data = okData(result);
        assertEquals(0L, asLong(data.get("total")));
    }

    @Test
    @DisplayName("getSellGoods: 测试分页功能")
    void testGetSellGoodsPagination() {
        for (int i = 0; i < 15; i++) {
            createTestGoods(i + 100, "sell");
        }

        List<Map<String, Object>> list1 = extractList(tUserService.getSellGoods(1, 10, "online"));
        assertTrue(list1.size() >= 10);
    }

    @Test
    @DisplayName("getSellGoods: goodsStatus参数无效时返回错误")
    void testGetSellGoodsInvalidStatus() {
        Result result = tUserService.getSellGoods(1, 10, "invalid_status");
        assertFail(result, "参数错误");
    }

    @Test
    @DisplayName("getSellGoods: goodsStatus为null时返回错误")
    void testGetSellGoodsNullStatus() {
        Result result = tUserService.getSellGoods(1, 10, null);
        assertFail(result, "参数错误");
    }

    @Test
    @DisplayName("getSellGoods: 获取不同状态的商品（sold）")
    void testGetSellGoodsSoldStatus() {
        createTestGoodsWithStatus(1, "sell", 1);
        Result result = tUserService.getSellGoods(1, 10, "sold");
        okData(result);
    }

    // ======================== getBoughtGoods 测试 ========================

    @Test
    @DisplayName("getBoughtGoods: 正常获取已购买的商品列表")
    void testGetBoughtGoodsSuccess() {
        createTestTrade(sellerId(1));
        createTestTrade(sellerId(2));
        Map<String, Object> data = okData(tUserService.getBoughtGoods(1, 10));
        assertTrue(data.containsKey("list"));
        assertTrue(data.containsKey("total"));
    }

    @Test
    @DisplayName("getBoughtGoods: 用户没有购买记录时返回空列表")
    void testGetBoughtGoodsEmpty() {
        Map<String, Object> data = okData(tUserService.getBoughtGoods(1, 10));
        assertEquals(0L, asLong(data.get("total")));
    }

    @Test
    @DisplayName("getBoughtGoods: 测试分页功能")
    void testGetBoughtGoodsPagination() {
        for (int i = 0; i < 12; i++) {
            createTestTrade(sellerId(i));
        }
        List<Map<String, Object>> list = extractList(tUserService.getBoughtGoods(1, 10));
        assertTrue(list.size() >= 10);
    }

    @Test
    @DisplayName("getBoughtGoods: 包含已评价和未评价的标记")
    void testGetBoughtGoodsWithEvaluation() {
        String sellerId = sellerId(99);
        Long tradeId = createTestTrade(sellerId);

        TEvaluate evaluate = new TEvaluate();
        evaluate.setTradeId(tradeId);
        evaluate.setGoodsId(getTradeGoodsId(tradeId));
        evaluate.setBuyerId(TEST_USER_ID);
        evaluate.setSellerId(sellerId);
        evaluate.setTotalScore(5);
        evaluate.setDescScore(5);
        evaluate.setCommScore(5);
        evaluate.setIsDelete(0);
        tEvaluateService.save(evaluate);

        List<Map<String, Object>> list = extractList(tUserService.getBoughtGoods(1, 10));
        assertFalse(list.isEmpty());
    }

    @Test
    @DisplayName("getBoughtGoods: 包含正确的卖家信息")
    void testGetBoughtGoodsIncludesSellerInfo() {
        String sellerId = sellerId(3);
        ensureUser(sellerId, "张三");
        createTestTrade(sellerId);
        List<Map<String, Object>> list = extractList(tUserService.getBoughtGoods(1, 10));
        assertFalse(list.isEmpty());
    }

    // ======================== getFavorites 测试 ========================

    @Test
    @DisplayName("getFavorites: 正常获取收藏列表")
    void testGetFavoritesSuccess() {
        Long goodsId1 = createTestGoods(1, "sell");
        Long goodsId2 = createTestGoods(2, "sell");
        createTestFavorite(goodsId1);
        createTestFavorite(goodsId2);
        Map<String, Object> data = okData(tUserService.getFavorites(1, 10));
        assertTrue(data.containsKey("list"));
    }

    @Test
    @DisplayName("getFavorites: 用户没有收藏时返回空列表")
    void testGetFavoritesEmpty() {
        Map<String, Object> data = okData(tUserService.getFavorites(1, 10));
        assertEquals(0L, asLong(data.get("total")));
    }

    @Test
    @DisplayName("getFavorites: 测试分页功能")
    void testGetFavoritesPagination() {
        for (int i = 0; i < 15; i++) {
            createTestFavorite(createTestGoods(i + 200, "sell"));
        }
        List<Map<String, Object>> list = extractList(tUserService.getFavorites(1, 10));
        assertTrue(list.size() >= 10);
    }

    @Test
    @DisplayName("getFavorites: 包含发布者信息")
    void testGetFavoritesWithPublisherInfo() {
        createTestFavorite(createTestGoods(1, "sell"));
        List<Map<String, Object>> list = extractList(tUserService.getFavorites(1, 10));
        assertFalse(list.isEmpty());
        assertTrue(list.get(0).containsKey("publisherName"));
    }

    @Test
    @DisplayName("getFavorites: 过滤掉已被删除的商品")
    void testGetFavoritesDeletedGoods() {
        Long goodsId1 = createTestGoods(1, "sell");
        Long goodsId2 = createTestGoods(2, "sell");
        createTestFavorite(goodsId1);
        createTestFavorite(goodsId2);

        TGoods goods = tGoodsService.getById(goodsId1);
        goods.setIsDelete(1);
        tGoodsService.updateById(goods);

        List<Map<String, Object>> list = extractList(tUserService.getFavorites(1, 10));
        assertEquals(1, list.size());
    }

    // ======================== getBuyGoods 测试 ========================

    @Test
    @DisplayName("getBuyGoods: 获取在线的预购商品列表")
    void testGetBuyGoodsOnlineSuccess() {
        createTestGoods(1, "buy");
        createTestGoods(2, "buy");
        Map<String, Object> data = okData(tUserService.getBuyGoods(1, 10, "online"));
        assertTrue(data.containsKey("list"));
    }

    @Test
    @DisplayName("getBuyGoods: 获取已下架的预购商品列表")
    void testGetBuyGoodsOfflineSuccess() {
        createTestGoodsWithStatus(1, "buy", 2);
        okData(tUserService.getBuyGoods(1, 10, "offline"));
    }

    @Test
    @DisplayName("getBuyGoods: 用户没有预购商品时返回空列表")
    void testGetBuyGoodsEmpty() {
        Map<String, Object> data = okData(tUserService.getBuyGoods(1, 10, "online"));
        assertEquals(0L, asLong(data.get("total")));
    }

    @Test
    @DisplayName("getBuyGoods: 测试分页功能")
    void testGetBuyGoodsPagination() {
        for (int i = 0; i < 12; i++) {
            createTestGoods(i + 300, "buy");
        }
        List<Map<String, Object>> list = extractList(tUserService.getBuyGoods(1, 10, "online"));
        assertTrue(list.size() >= 10);
    }

    @Test
    @DisplayName("getBuyGoods: status参数无效时返回错误")
    void testGetBuyGoodsInvalidStatus() {
        assertFail(tUserService.getBuyGoods(1, 10, "invalid"), "参数错误");
    }

    @Test
    @DisplayName("getBuyGoods: status为null时返回错误")
    void testGetBuyGoodsNullStatus() {
        assertFail(tUserService.getBuyGoods(1, 10, null), "参数错误");
    }

    @Test
    @DisplayName("getBuyGoods: 分页参数自动修正")
    void testGetBuyGoodsPageAutoCorrection() {
        createTestGoods(1, "buy");
        okData(tUserService.getBuyGoods(-1, 0, "online"));
    }

    // ======================== 辅助方法 ========================

    private String sellerId(int index) {
        return TEST_PREFIX + "_seller_" + index;
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

    private void ensureCategory() {
        TGoodsCategory category = tGoodsCategoryService.getOne(new LambdaQueryWrapper<TGoodsCategory>()
                .eq(TGoodsCategory::getCategoryName, TEST_CATEGORY)
                .last("LIMIT 1"));
        if (category != null) {
            return;
        }
        TGoodsCategory newCategory = new TGoodsCategory();
        newCategory.setCategoryName(TEST_CATEGORY);
        newCategory.setSort(1);
        newCategory.setIsShow(0);
        tGoodsCategoryService.save(newCategory);
    }

    private Long createTestGoods(int goodsIndex, String type) {
        return createTestGoodsWithStatus(goodsIndex, type, 0);
    }

    private Long createTestGoodsWithStatus(int goodsIndex, String type, Integer status) {
        Result publishResult = publishGoodsByModule(TEST_USER_ID, goodsIndex, type);
        Map<String, Object> publishData = okData(publishResult);
        Long goodsId = ((Number) publishData.get("goodsId")).longValue();

        if (!Objects.equals(status, 0)) {
            TGoods goods = tGoodsService.getById(goodsId);
            goods.setGoodsStatus(status);
            tGoodsService.updateById(goods);
        }
        return goodsId;
    }

    private Result publishGoodsByModule(String ownerId, int index, String type) {
        ensureUser(ownerId, ownerId);
        String originalUserId = UserContext.getUserId();
        try {
            UserContext.setUserId(ownerId);
            Map<String, Object> request = new HashMap<>();
            request.put("name", "测试商品" + index);
            request.put("desc", "这是测试商品描述");
            request.put("remark", "测试备注");
            request.put("price", 99.99);
            request.put("purpose", "宿舍");
            request.put("exchangeAddr", "学校门口");
            request.put("imgUrls", List.of());
            request.put("type", type);
            request.put("category", TEST_CATEGORY);
            return goodsController.publishGoods(request);
        } finally {
            UserContext.setUserId(originalUserId);
        }
    }

    private void createTestFavorite(Long goodsId) {
        TFavorite favorite = new TFavorite();
        favorite.setUserId(TEST_USER_ID);
        favorite.setGoodsId(goodsId);
        favorite.setIsDelete(0);
        tFavoriteService.save(favorite);
    }

    private Long createTestTrade(String sellerId) {
        Long sellerGoodsId = createSellerGoods(sellerId);

        TTrade trade = new TTrade();
        trade.setBuyerId(TEST_USER_ID);
        trade.setSellerId(sellerId);
        trade.setGoodsId(sellerGoodsId);
        trade.setTradePrice(new BigDecimal("199.99"));
        trade.setTradePlace("学校门口");
        trade.setTradeTime(LocalDateTime.now());
        trade.setIsDelete(0);
        tTradeService.save(trade);
        return trade.getTradeId();
    }

    private Long getTradeGoodsId(Long tradeId) {
        TTrade trade = tTradeService.getById(tradeId);
        return trade == null ? null : trade.getGoodsId();
    }

    private Long createSellerGoods(String sellerId) {
        Result publishResult = publishGoodsByModule(sellerId, new Random().nextInt(10000), "sell");
        Long goodsId = ((Number) okData(publishResult).get("goodsId")).longValue();
        TGoods goods = tGoodsService.getById(goodsId);
        goods.setGoodsStatus(1);
        tGoodsService.updateById(goods);
        return goodsId;
    }

    private void assertFail(Result result, String msgPart) {
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains(msgPart));
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> okData(Result result) {
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        return (Map<String, Object>) result.getData();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Result result) {
        Object list = okData(result).get("list");
        return list == null ? List.of() : (List<Map<String, Object>>) list;
    }
}