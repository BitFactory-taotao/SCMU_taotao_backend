package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.goods.GoodsResponseDTO;
import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import com.bit.scmu_taotao.dto.goods.SearchRequestDTO;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TFavorite;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TGoodsCategory;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.TFavoriteService;
import com.bit.scmu_taotao.service.TGoodsCategoryService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品控制器集成测试
 * 覆盖 GoodsController 首页列表/搜索/发布/详情/收藏/联系/草稿 等接口
 */
@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("商品控制器集成测试")
class GoodsControllerTest {

    @Autowired
    private GoodsController goodsController;

    @Autowired
    private TGoodsService goodsService;

    @Autowired
    private TUserService userService;

    @Autowired
    private TGoodsCategoryService categoryService;

    @Autowired
    private TFavoriteService favoriteService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    private static final String PREFIX = "ut_goods_ctrl";
    private static final String BUYER = PREFIX + "_buyer";
    private static final String SELLER = PREFIX + "_seller";
    /** 发布商品与首页 dormitory tab 共用的分类名（dormitory 映射到该分类） */
    private static final String CATEGORY = "宿舍用品";

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(BUYER, "测试买家");
        ensureUser(SELLER, "测试卖家");
        ensureCategory();
        UserContext.setUserId(BUYER);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
        cleanTestData();
    }

    // ======================== 辅助方法 ========================

    private void cleanTestData() {
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .likeRight(ChatMessage::getSendId, PREFIX + "_")
                .or().likeRight(ChatMessage::getReceiveId, PREFIX + "_"));
        chatSessionService.remove(new LambdaQueryWrapper<ChatSession>()
                .likeRight(ChatSession::getUser1Id, PREFIX + "_")
                .or().likeRight(ChatSession::getUser2Id, PREFIX + "_"));
        favoriteService.remove(new LambdaQueryWrapper<TFavorite>()
                .likeRight(TFavorite::getUserId, PREFIX + "_"));
        goodsService.remove(new LambdaQueryWrapper<TGoods>()
                .likeRight(TGoods::getUserId, PREFIX + "_"));
        userService.remove(new LambdaQueryWrapper<TUser>()
                .likeRight(TUser::getUserId, PREFIX + "_"));
    }

    private void ensureUser(String userId, String name) {
        if (userService.getById(userId) != null) {
            return;
        }
        TUser u = new TUser();
        u.setUserId(userId);
        u.setUserName(name);
        u.setAvatar("https://example.com/avatar.png");
        u.setCreditScore(100);
        u.setCreditStar(java.math.BigDecimal.valueOf(4.0));
        u.setIsDelete(0);
        userService.save(u);
    }

    private void ensureCategory() {
        TGoodsCategory category = categoryService.getOne(new LambdaQueryWrapper<TGoodsCategory>()
                .eq(TGoodsCategory::getCategoryName, CATEGORY)
                .last("LIMIT 1"));
        if (category != null) {
            return;
        }
        TGoodsCategory newCategory = new TGoodsCategory();
        newCategory.setCategoryName(CATEGORY);
        newCategory.setSort(1);
        newCategory.setIsShow(0);
        categoryService.save(newCategory);
    }

    /** 以指定 owner 身份发布商品，返回 goodsId */
    private Long publishGoods(String ownerId, String name, String type) {
        ensureUser(ownerId, ownerId);
        String originalUserId = UserContext.getUserId();
        try {
            UserContext.setUserId(ownerId);
            Map<String, Object> request = new HashMap<>();
            request.put("name", name);
            request.put("desc", "测试商品描述");
            request.put("remark", "测试备注");
            request.put("price", 66.6);
            request.put("purpose", "宿舍使用");
            request.put("exchangeAddr", "宿舍楼下");
            request.put("imgUrls", List.of());
            request.put("type", type);
            request.put("category", CATEGORY);
            Result result = goodsController.publishGoods(request);
            Map<String, Object> data = okData(result);
            return ((Number) data.get("goodsId")).longValue();
        } finally {
            UserContext.setUserId(originalUserId);
        }
    }

    private BindingResult bind(Object target) {
        return new BeanPropertyBindingResult(target, "query");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> okData(Result result) {
        assertNotNull(result);
        assertEquals(200, result.getCode(), "返回: " + result.getMsg());
        assertNotNull(result.getData(), "data 为空: " + result.getMsg());
        return (Map<String, Object>) result.getData();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Result result) {
        Object list = okData(result).get("list");
        return list == null ? List.of() : (List<Map<String, Object>>) list;
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }

    // ======================== 发布商品 ========================

    @Test
    @DisplayName("publishGoods: 买家成功发布出售商品并返回 goodsId")
    void testPublishSellGoods() {
        Long goodsId = publishGoods(BUYER, "UT-出售台灯", "sell");
        assertNotNull(goodsId);
        TGoods goods = goodsService.getById(goodsId);
        assertNotNull(goods);
        assertEquals(BUYER, goods.getUserId());
        assertEquals(1, goods.getGoodsType());
    }

    @Test
    @DisplayName("publishGoods: 成功发布预购商品")
    void testPublishBuyGoods() {
        Long goodsId = publishGoods(BUYER, "UT-预购自行车", "buy");
        assertNotNull(goodsId);
        TGoods goods = goodsService.getById(goodsId);
        assertEquals(2, goods.getGoodsType());
    }

    @Test
    @DisplayName("publishGoods: 未登录时返回 401")
    void testPublishWithoutLogin() {
        UserContext.remove();
        Map<String, Object> request = new HashMap<>();
        request.put("name", "未登录商品");
        request.put("type", "sell");
        request.put("category", CATEGORY);
        request.put("price", 10.0);
        request.put("imgUrls", List.of());
        Result result = goodsController.publishGoods(request);
        assertEquals(401, result.getCode());
    }

    // ======================== 商品详情 ========================

    @Test
    @DisplayName("getGoods: 获取已发布商品详情")
    void testGetGoodsDetail() {
        Long goodsId = publishGoods(SELLER, "UT-详情商品", "sell");

        Result result = goodsController.getGoods(goodsId.intValue());
        assertEquals(200, result.getCode());
        assertTrue(result.getData() instanceof GoodsResponseDTO);
        GoodsResponseDTO dto = (GoodsResponseDTO) result.getData();
        assertEquals("UT-详情商品", dto.getName());
        assertEquals("sell", dto.getType());
        assertNotNull(dto.getPublisher());
        assertNotNull(dto.getImgUrls());
    }

    @Test
    @DisplayName("getGoods: 详情包含发布者信息")
    void testGetGoodsPublisher() {
        Long goodsId = publishGoods(SELLER, "UT-带发布者商品", "sell");

        Result result = goodsController.getGoods(goodsId.intValue());
        assertEquals(200, result.getCode());
        assertTrue(result.getData() instanceof GoodsResponseDTO);
        GoodsResponseDTO dto = (GoodsResponseDTO) result.getData();
        PublisherDTO publisher = dto.getPublisher();
        assertNotNull(publisher);
        assertEquals(SELLER, publisher.getId());
        assertEquals("测试卖家", publisher.getName());
    }

    // ======================== 首页模糊搜索 ========================

    @Test
    @DisplayName("searchHomeGoods: 按关键词命中在售商品")
    void testSearchHit() {
        publishGoods(SELLER, "UT-苹果手机壳", "sell");

        SearchRequestDTO query = new SearchRequestDTO();
        query.setKeyword("手机壳");
        Result result = goodsController.searchHomeGoods(query, bind(query));
        assertEquals(200, result.getCode());

        List<Map<String, Object>> list = extractList(result);
        assertTrue(list.stream().anyMatch(row -> "UT-苹果手机壳".equals(row.get("name"))),
                "搜索结果应命中关键词商品");
    }

    @Test
    @DisplayName("searchHomeGoods: 无匹配关键词返回空列表")
    void testSearchNoMatch() {
        SearchRequestDTO query = new SearchRequestDTO();
        query.setKeyword("绝不存在的关键词xyz");
        Result result = goodsController.searchHomeGoods(query, bind(query));
        assertEquals(200, result.getCode());
        assertTrue(extractList(result).isEmpty());
    }

    @Test
    @DisplayName("searchHomeGoods: 空关键词返回 400")
    void testSearchEmptyKeyword() {
        SearchRequestDTO query = new SearchRequestDTO();
        query.setKeyword("");
        Result result = goodsController.searchHomeGoods(query, bind(query));
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("searchHomeGoods: 列表项字段齐全")
    void testSearchRowFields() {
        publishGoods(SELLER, "UT-字段齐全商品", "sell");

        SearchRequestDTO query = new SearchRequestDTO();
        query.setKeyword("字段齐全");
        Result result = goodsController.searchHomeGoods(query, bind(query));

        Map<String, Object> row = extractList(result).stream()
                .filter(r -> "UT-字段齐全商品".equals(r.get("name")))
                .findFirst().orElse(null);
        assertNotNull(row);
        assertTrue(row.containsKey("id"));
        assertTrue(row.containsKey("price"));
        assertTrue(row.containsKey("imgUrl"));
        assertTrue(row.containsKey("publishTime"));
        assertTrue(row.containsKey("publisherName"));
        assertTrue(row.containsKey("publisherId"));
    }

    // ======================== 首页商品列表 ========================

    @Test
    @DisplayName("getHomeGoodsList: dormitory 分类返回对应商品")
    void testHomeListDormitory() {
        Long sellId = publishGoods(SELLER, "UT-宿舍洗衣机", "sell");
        publishGoods(SELLER, "UT-娱乐耳机", "sell"); // 该商品也属于宿舍用品分类，仅验证可查到

        Result result = goodsController.getHomeGoodsList("dormitory", null, 1, 10);
        assertEquals(200, result.getCode());
        List<Map<String, Object>> list = extractList(result);
        assertTrue(list.stream().anyMatch(row -> asLong(row.get("id")) == sellId),
                "dormitory 首页应包含分类内商品");
    }

    @Test
    @DisplayName("getHomeGoodsList: pre-order 返回预购商品")
    void testHomeListPreOrder() {
        publishGoods(SELLER, "UT-在售台灯", "sell");
        Long buyId = publishGoods(BUYER, "UT-预购游戏机", "buy");

        Result result = goodsController.getHomeGoodsList("pre-order", null, 1, 10);
        List<Map<String, Object>> list = extractList(result);
        assertTrue(list.stream().anyMatch(row -> asLong(row.get("id")) == buyId),
                "pre-order 首页应包含预购商品");
        assertTrue(list.stream().noneMatch(row -> "UT-在售台灯".equals(row.get("name"))),
                "pre-order 不应包含普通出售商品");
    }

    @Test
    @DisplayName("getHomeGoodsList: 非法 tab 返回 400")
    void testHomeListInvalidTab() {
        Result result = goodsController.getHomeGoodsList("not_a_tab", null, 1, 10);
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("getHomeGoodsList: 列表项字段齐全")
    void testHomeListRowFields() {
        publishGoods(SELLER, "UT-首页列表商品", "sell");

        Result result = goodsController.getHomeGoodsList("dormitory", null, 1, 10);
        Map<String, Object> row = extractList(result).stream()
                .filter(r -> "UT-首页列表商品".equals(r.get("name")))
                .findFirst().orElse(null);
        assertNotNull(row, "首页应包含刚发布商品");
        assertTrue(row.containsKey("id"));
        assertTrue(row.containsKey("name"));
        assertTrue(row.containsKey("remark"));
        assertTrue(row.containsKey("price"));
        assertTrue(row.containsKey("imgUrl"));
        assertTrue(row.containsKey("publishTime"));
        assertTrue(row.containsKey("publisherName"));
        assertTrue(row.containsKey("publisherId"));
    }

    // ======================== 收藏 ========================

    @Test
    @DisplayName("favorite: 收藏商品成功")
    void testFavorite() {
        Long goodsId = publishGoods(SELLER, "UT-待收藏商品", "sell");

        Result result = goodsController.favorite(goodsId.intValue());
        assertEquals(200, result.getCode());

        TFavorite fav = favoriteService.getOne(new LambdaQueryWrapper<TFavorite>()
                .eq(TFavorite::getUserId, BUYER)
                .eq(TFavorite::getGoodsId, goodsId));
        assertNotNull(fav);
        assertEquals(0, fav.getIsDelete());
    }

    @Test
    @DisplayName("favorite: 重复收藏幂等返回成功")
    void testFavoriteDuplicate() {
        Long goodsId = publishGoods(SELLER, "UT-幂等收藏商品", "sell");

        Result first = goodsController.favorite(goodsId.intValue());
        assertEquals(200, first.getCode());
        Result second = goodsController.favorite(goodsId.intValue());
        assertEquals(200, second.getCode());
        assertTrue(second.getMsg().contains("已收藏") || second.getMsg().contains("收藏成功"));
    }

    @Test
    @DisplayName("unfavorite: 取消收藏成功并可再次收藏")
    void testUnfavorite() {
        Long goodsId = publishGoods(SELLER, "UT-取消收藏商品", "sell");
        goodsController.favorite(goodsId.intValue());

        Result result = goodsController.unfavorite(goodsId.intValue());
        assertEquals(200, result.getCode());

        TFavorite fav = favoriteService.getOne(new LambdaQueryWrapper<TFavorite>()
                .eq(TFavorite::getUserId, BUYER)
                .eq(TFavorite::getGoodsId, goodsId));
        assertEquals(1, fav.getIsDelete());
    }

    // ======================== 联系商家 ========================

    @Test
    @DisplayName("contactSeller: 买家联系卖家创建会话")
    void testContactSeller() {
        Long goodsId = publishGoods(SELLER, "UT-联系商品", "sell");

        Result result = goodsController.contactSeller(goodsId.intValue());
        Map<String, Object> data = okData(result);
        assertNotNull(data.get("chatId"));
        assertEquals(SELLER, data.get("sellerId"));

        ChatSession session = chatSessionService.getById(((Number) data.get("chatId")).longValue());
        assertNotNull(session);
        assertTrue(session.getUser1Id().equals(BUYER) || session.getUser2Id().equals(BUYER));
        assertTrue(session.getUser1Id().equals(SELLER) || session.getUser2Id().equals(SELLER));
    }

    @Test
    @DisplayName("contactSeller: 联系不存在商品返回 404")
    void testContactSellerNotFound() {
        Result result = goodsController.contactSeller(999999999);
        assertNotEquals(200, result.getCode());
    }

    // ======================== 草稿 ========================

    @Test
    @DisplayName("草稿: 新建→查询详情→删除 全流程")
    void testDraftLifecycle() {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "UT-草稿商品");
        request.put("desc", "草稿描述");
        request.put("remark", "草稿备注");
        request.put("price", 88.8);
        request.put("purpose", "测试");
        request.put("exchangeAddr", "学校");
        request.put("imgUrls", List.of());
        request.put("type", "sell");

        Result saveResult = goodsController.saveDraft(request);
        Map<String, Object> saveData = okData(saveResult);
        String draftId = (String) saveData.get("draftId");
        assertNotNull(draftId);

        Result detailResult = goodsController.getDraftDetail(draftId);
        assertEquals(200, detailResult.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> draft = (Map<String, Object>) detailResult.getData();
        assertNotNull(draft.get("draft"));

        Result deleteResult = goodsController.deleteDraft(draftId);
        assertEquals(200, deleteResult.getCode());

        Result afterDelete = goodsController.getDraftDetail(draftId);
        assertEquals(404, afterDelete.getCode());
    }

    @Test
    @DisplayName("草稿: 更新已有草稿")
    void testDraftUpdate() {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "UT-更新前草稿");
        request.put("price", 10.0);
        request.put("type", "sell");
        request.put("imgUrls", List.of());
        Result saveResult = goodsController.saveDraft(request);
        String draftId = (String) okData(saveResult).get("draftId");

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("draftId", draftId);
        updateRequest.put("name", "UT-更新后草稿");
        updateRequest.put("price", 20.0);
        updateRequest.put("type", "sell");
        updateRequest.put("imgUrls", List.of());
        Result updateResult = goodsController.saveDraft(updateRequest);
        assertEquals(200, updateResult.getCode());
    }
}
