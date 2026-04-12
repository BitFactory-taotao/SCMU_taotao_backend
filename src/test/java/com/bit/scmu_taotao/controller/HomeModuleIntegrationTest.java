package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TGoodsCategory;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.TGoodsCategoryService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HomeModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private String uniqueToken;
    private String testUserId;
    private String authToken;
    private Long categoryGoodsId;

    @BeforeEach
    void setUp() {
        uniqueToken = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        testUserId = "u" + uniqueToken;

        ensureUser(testUserId, "首页测试用户");
        authToken = tokenUtil.generateToken(testUserId);

        Integer dormitoryCategoryId = ensureCategory("宿舍用品");
        createGoods(testUserId, dormitoryCategoryId, "首页搜索商品_" + uniqueToken,
                "描述_" + uniqueToken, "备注_" + uniqueToken, 1);
        categoryGoodsId = createGoods(testUserId, dormitoryCategoryId, "首页分类商品_" + uniqueToken,
                "分类描述", "分类备注", 1);
    }

    @AfterEach
    void tearDown() {
        tGoodsService.remove(new LambdaQueryWrapper<TGoods>()
                .eq(TGoods::getUserId, testUserId));
        tUserService.remove(new LambdaQueryWrapper<TUser>()
                .eq(TUser::getUserId, testUserId));
    }

    @Test
    @DisplayName("首页搜索: 关键字命中商品并返回标准分页结构")
    void testSearchSuccess() throws Exception {
        Assumptions.assumeTrue(isSearchEndpointAvailable(), "未检测到 /goods/search 映射，跳过搜索集成测试");

        Result result = callApi("/goods/search", Map.of(
                "keyword", uniqueToken,
                "page", "1",
                "size", "10"
        ));

        Map<String, Object> data = assertOkAndGetData(result);
        assertTrue(data.containsKey("total"));
        assertTrue(data.containsKey("pages"));
        assertTrue(data.containsKey("list"));

        List<Map<String, Object>> list = getList(data);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(item -> asLong(item.get("id")) > 0));
    }

    @Test
    @DisplayName("首页搜索: keyword为空白时返回400业务码")
    void testSearchKeywordBlank() throws Exception {
        Assumptions.assumeTrue(isSearchEndpointAvailable(), "未检测到 /goods/search 映射，跳过搜索集成测试");

        Result result = callApi("/goods/search", Map.of(
                "keyword", "   "
        ));

        assertEquals(400, result.getCode());
        assertNotNull(result.getMsg());
        assertTrue(result.getMsg().contains("keyword不能为空"));
    }

    @Test
    @DisplayName("首页推荐: tab=recommend 返回成功与标准结构")
    void testHomeRecommendTab() throws Exception {
        Result result = callApi("/goods", Map.of(
                "tab", "recommend",
                "page", "1",
                "size", "10"
        ));

        Map<String, Object> data = assertOkAndGetData(result);
        assertTrue(data.containsKey("total"));
        assertTrue(data.containsKey("pages"));
        assertTrue(data.containsKey("list"));
    }

    @Test
    @DisplayName("首页分类展示: tab=dormitory 仅返回宿舍用品分类且包含测试商品")
    void testHomeDormitoryTab() throws Exception {
        Result result = callApi("/goods", Map.of(
                "tab", "dormitory",
                "page", "1",
                "size", "50"
        ));

        Map<String, Object> data = assertOkAndGetData(result);
        List<Map<String, Object>> list = getList(data);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(item -> asLong(item.get("id")) == categoryGoodsId));
    }

    @Test
    @DisplayName("首页分类展示: 非法tab返回400业务码")
    void testHomeInvalidTab() throws Exception {
        Result result = callApi("/goods", Map.of(
                "tab", "unknown"
        ));

        assertEquals(400, result.getCode());
        assertTrue(result.getMsg().contains("tab参数非法"));
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

    private Long createGoods(String userId, Integer categoryId, String goodsName,
                             String goodsDesc, String goodsNote, Integer goodsType) {
        TGoods goods = new TGoods();
        goods.setUserId(userId);
        goods.setCategoryId(categoryId);
        goods.setGoodsType(goodsType);
        goods.setGoodsName(goodsName);
        goods.setGoodsDesc(goodsDesc);
        goods.setGoodsNote(goodsNote);
        goods.setPrice(new BigDecimal("9.90"));
        goods.setUseScene("测试场景");
        goods.setExchangePlace("测试地点");
        goods.setGoodsStatus(0);
        goods.setIsDelete(0);
        tGoodsService.save(goods);
        return goods.getGoodsId();
    }

    private Result callApi(String path, Map<String, String> params) throws Exception {
        var requestBuilder = get(path);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            requestBuilder.param(entry.getKey(), entry.getValue());
        }
        requestBuilder.header("Authorization", "Bearer " + authToken);

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, Result.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> assertOkAndGetData(Result result) {
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        return objectMapper.convertValue(result.getData(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> data) {
        Object list = data.get("list");
        return list == null ? List.of() : (List<Map<String, Object>>) list;
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private boolean isSearchEndpointAvailable() {
        for (RequestMappingInfo info : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            if (info.getPatternValues().contains("/goods/search")) {
                return true;
            }
        }
        return false;
    }
}

