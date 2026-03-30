//package com.bit.scmu_taotao.controller;
//
//import com.bit.scmu_taotao.util.UserContext;
//import com.bit.scmu_taotao.util.common.Result;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//public class GoodsControllerTest {
//
//    private static final String USER_ID = "202421091019";
//    private static final String TEST_IMAGE_PATH = "src/test/java/com/bit/scmu_taotao/images/";
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    @Autowired
//    private WebApplicationContext webApplicationContext;
//    @Autowired
//    private com.bit.scmu_taotao.util.TokenUtil tokenUtil;
//    private MockMvc mockMvc;
//    private String token;
//
//    @BeforeEach
//    void setUp() {
//        UserContext.setUserId(USER_ID);
//        // 生成token并添加到请求头
//        token = "token_1774685487587758061";
//        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
//    }
//
//    private String buildGoodsJson(String name, String desc, String remark, double price,
//                                  String purpose, String exchangeAddr, List<String> imgUrls, String type) throws Exception {
//        Map<String, Object> req = new HashMap<>();
//        req.put("name", name);
//        req.put("desc", desc);
//        req.put("remark", remark);
//        req.put("price", price);
//        req.put("purpose", purpose);
//        req.put("exchangeAddr", exchangeAddr);
//        req.put("imgUrls", imgUrls);
//        req.put("type", type);
//        return objectMapper.writeValueAsString(req);
//    }
//
//    private Map<String, Object> parseResultData(String responseContent) throws Exception {
//        Result resultObj = objectMapper.readValue(responseContent, Result.class);
//        assertNotNull(resultObj, "响应反序列化失败: " + responseContent);
//        System.out.println(resultObj);
//        assertEquals(200, resultObj.getCode(), "接口返回非200: " + responseContent);
//        assertNotNull(resultObj.getData(), "接口 data 为空: " + responseContent);
//
//        return objectMapper.convertValue(
//                resultObj.getData(),
//                new TypeReference<Map<String, Object>>() {
//                }
//        );
//    }
//
//    private Integer publishAndGetGoodsId() throws Exception {
//        // 硬编码图片URL
//        List<String> imgUrls = new java.util.ArrayList<>();
//        imgUrls.add("http://localhost:9000/taotao/user/202421091019/2026/03/27/792f825ddf6e461d82dbb7556292e8b1.jpg");
//        imgUrls.add("http://localhost:9000/taotao/user/202421091019/2026/03/27/850ef7f9b9e843cf802de0156a02babf.jpg");
//
//        MvcResult publishResult = mockMvc.perform(MockMvcRequestBuilders.post("/goods")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(buildGoodsJson(
//                                "测试商品", "测试商品描述", "测试商品备注", 100.0,
//                                "测试用途", "测试地址", imgUrls, "sell"
//                        ))
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String publishResponse = publishResult.getResponse().getContentAsString();
//        Map<String, Object> data = parseResultData(publishResponse);
//        Integer goodsId = objectMapper.convertValue(data.get("goodsId"), Integer.class);
//        assertNotNull(goodsId, "Goods ID should not be null");
//        return goodsId;
//    }
//
//
//    @Test
//    void testPublishGoods() throws Exception {
//        List<String> imgUrls = uploadImages();
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/goods")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(buildGoodsJson(
//                                "测试商品", "测试商品描述", "测试商品备注", 100.0,
//                                "测试用途", "测试地址", imgUrls, "sell"
//                        ))
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("发布成功"));
//    }
//
//    @Test
//    void testGetDraftList() throws Exception {
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/goods/draft")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("获取草稿列表成功"));
//    }
//
//    @Test
//    void testGetDraftDetail() throws Exception {
//        String draftId = "47b0964f-d379-43ab-b0cf-013dfeed5aa2";
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/goods/draft/" + draftId)
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("获取草稿详情成功"));
//    }
//
//    @Test
//    void testDeleteDraft() throws Exception {
//        String draftId = "47b0964f-d379-43ab-b0cf-013dfeed5aa2";
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/goods/draft/" + draftId)
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("删除草稿成功"));
//    }
//
//    @Test
//    void testGetGoods() throws Exception {
//        Integer goodsId = publishAndGetGoodsId();
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/goods/" + goodsId)
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("请求成功"));
//    }
//
//    @Test
//    void testFavorite() throws Exception {
//        Integer goodsId = publishAndGetGoodsId();
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/goods/" + goodsId + "/favorite")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("收藏成功"));
//    }
//
//    @Test
//    void testContactSeller() throws Exception {
//        Integer goodsId = publishAndGetGoodsId();
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/goods/" + goodsId + "/trade")
//                        .header("Authorization", "Bearer " + token))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        assertTrue(responseContent.contains("联系商家成功"));
//    }
//}