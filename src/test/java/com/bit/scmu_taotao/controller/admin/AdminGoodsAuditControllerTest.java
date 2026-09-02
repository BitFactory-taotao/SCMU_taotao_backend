package com.bit.scmu_taotao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.GoodsAuditApproveRequest;
import com.bit.scmu_taotao.dto.admin.GoodsAuditDetailDTO;
import com.bit.scmu_taotao.dto.admin.GoodsAuditListItemDTO;
import com.bit.scmu_taotao.dto.admin.GoodsAuditPageRequest;
import com.bit.scmu_taotao.dto.admin.GoodsAuditRejectRequest;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.StompPushService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品巡检审核模块集成测试
 * 覆盖 /admin/goods/audit 下 list/detail/approve/reject 四个接口
 */
@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("商品巡检审核集成测试")
class AdminGoodsAuditControllerTest {

    @Autowired
    private AdminGoodsAuditController controller;

    @Autowired
    private TGoodsService goodsService;

    @Autowired
    private TUserService userService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @MockBean
    private StompPushService stompPushService;

    private static final String PREFIX = "ut_goods_audit";
    private static final String PUBLISHER = PREFIX + "_pub";

    private Long pending1Id;
    private Long pending2Id;
    private Long approvedId;
    private Long rejectedId;

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(PUBLISHER, "巡检商品发布者");

        pending1Id = insertGoods(PUBLISHER, "待巡检-考研资料", 0);
        pending2Id = insertGoods(PUBLISHER, "待巡检-二手自行车", null);
        approvedId = insertGoods(PUBLISHER, "已通过-台灯", 1);
        rejectedId = insertGoods(PUBLISHER, "已驳回-违规商品", 2);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    // ======================== 辅助方法 ========================

    private void cleanTestData() {
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSendId, "system")
                .likeRight(ChatMessage::getReceiveId, PREFIX + "_"));
        chatSessionService.remove(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUser1Id, "system")
                .likeRight(ChatSession::getUser2Id, PREFIX + "_"));
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
        u.setCreditStar(BigDecimal.valueOf(4.0));
        u.setIsDelete(0);
        userService.save(u);
    }

    private Long insertGoods(String userId, String name, Integer isAudited) {
        TGoods g = new TGoods();
        g.setUserId(userId);
        g.setGoodsName(name);
        g.setGoodsDesc("测试商品描述");
        g.setGoodsNote("测试备注");
        g.setCategoryId(1);
        g.setGoodsType(1);
        g.setPrice(BigDecimal.valueOf(99.9));
        g.setUseScene("学习");
        g.setExchangePlace("图书馆");
        g.setGoodsStatus(0);
        g.setIsAudited(isAudited);
        g.setViewCount(0);
        g.setIsDelete(0);
        g.setCreateTime(LocalDateTime.now());
        g.setUpdateTime(LocalDateTime.now());
        goodsService.save(g);
        return g.getGoodsId();
    }

    private BindingResult bind(Object target) {
        return new BeanPropertyBindingResult(target, "request");
    }

    private GoodsAuditPageRequest pageReq(Integer auditStatus, String category, String keyword) {
        GoodsAuditPageRequest req = new GoodsAuditPageRequest();
        req.setAuditStatus(auditStatus);
        req.setCategory(category);
        req.setKeyword(keyword);
        req.setPage(1);
        req.setSize(10);
        return req;
    }

    // ======================== GET /admin/goods/audit/list ========================

    @Nested
    @DisplayName("GET /admin/goods/audit/list — 商品巡检列表")
    class ListTests {

        @Test
        @DisplayName("auditStatus=0 仅返回待巡检商品（isAudited=0 或 null）")
        void listPendingOnly() {
            Result result = controller.list(pageReq(0, null, null), bind(pageReq(0, null, null)));
            assertEquals(200, result.getCode(), "返回: " + result.getMsg());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertNotNull(data);
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            assertNotNull(list);
            assertFalse(list.isEmpty(), "待巡检列表不应为空");
            assertTrue(list.stream().anyMatch(i -> i.getId().equals(pending1Id)), "应包含 isAudited=0 商品");
            assertTrue(list.stream().anyMatch(i -> i.getId().equals(pending2Id)), "应包含 isAudited=null 商品");
            assertFalse(list.stream().anyMatch(i -> i.getId().equals(approvedId)), "已通过商品不应出现");
            assertFalse(list.stream().anyMatch(i -> i.getId().equals(rejectedId)), "已驳回商品不应出现");
        }

        @Test
        @DisplayName("auditStatus 未传时默认按待巡检(0)过滤")
        void listDefaultPending() {
            GoodsAuditPageRequest req = new GoodsAuditPageRequest();
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getId().equals(pending1Id)));
            assertTrue(list.stream().noneMatch(i -> i.getId().equals(approvedId)));
        }

        @Test
        @DisplayName("auditStatus=1 仅返回已通过商品")
        void listApprovedOnly() {
            Result result = controller.list(pageReq(1, null, null), bind(pageReq(1, null, null)));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getId().equals(approvedId)));
            assertFalse(list.stream().anyMatch(i -> i.getId().equals(pending1Id)), "待巡检商品不应出现");
        }

        @Test
        @DisplayName("auditStatus=2 仅返回驳回下架商品")
        void listRejectedOnly() {
            Result result = controller.list(pageReq(2, null, null), bind(pageReq(2, null, null)));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getId().equals(rejectedId)));
            assertFalse(list.stream().anyMatch(i -> i.getId().equals(pending1Id)));
        }

        @Test
        @DisplayName("auditStatus 越界返回 400")
        void listInvalidAuditStatus() {
            Result result = controller.list(pageReq(99, null, null), bind(pageReq(99, null, null)));
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("keyword 按商品名称模糊匹配")
        void listKeywordMatchesName() {
            Result result = controller.list(pageReq(0, null, "考研"), bind(pageReq(0, null, "考研")));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getId().equals(pending1Id)), "应命中名称含'考研'的商品");
            assertFalse(list.stream().anyMatch(i -> i.getId().equals(pending2Id)), "不应命中无关商品");
        }

        @Test
        @DisplayName("keyword 支持按发布者学号匹配")
        void listKeywordMatchesUserId() {
            Result result = controller.list(pageReq(null, null, PUBLISHER), bind(pageReq(null, null, PUBLISHER)));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getPublisherId().equals(PUBLISHER)));
        }

        @Test
        @DisplayName("非法 category 返回 400")
        void listInvalidCategory() {
            Result result = controller.list(pageReq(0, "not_a_category", null), bind(pageReq(0, "not_a_category", null)));
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("列表项字段齐全：name/remark/price/imgUrl/publishTime/publisherName")
        void listItemFieldsPopulated() {
            Result result = controller.list(pageReq(1, null, null), bind(pageReq(1, null, null)));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<GoodsAuditListItemDTO> list = (List<GoodsAuditListItemDTO>) data.get("list");
            GoodsAuditListItemDTO item = list.stream()
                    .filter(i -> i.getId().equals(approvedId))
                    .findFirst().orElse(null);
            assertNotNull(item, "已通过商品应在列表");
            assertEquals("已通过-台灯", item.getName());
            assertEquals("测试备注", item.getRemark());
            assertEquals(0, new BigDecimal("99.9").compareTo(item.getPrice()));
            assertNotNull(item.getImgUrl());
            assertNotNull(item.getPublishTime());
            assertEquals("巡检商品发布者", item.getPublisherName());
            assertEquals(PUBLISHER, item.getPublisherId());
        }
    }

    // ======================== GET /admin/goods/audit/{goodsId} ========================

    @Nested
    @DisplayName("GET /admin/goods/audit/{goodsId} — 巡检详情")
    class DetailTests {

        @Test
        @DisplayName("查看有效商品详情返回完整字段")
        void detailSuccess() {
            Result result = controller.detail(pending1Id);
            assertEquals(200, result.getCode());

            assertTrue(result.getData() instanceof GoodsAuditDetailDTO);
            GoodsAuditDetailDTO dto = (GoodsAuditDetailDTO) result.getData();
            assertEquals(pending1Id.longValue(), dto.getId().longValue());
            assertEquals("待巡检-考研资料", dto.getName());
            assertEquals("sell", dto.getType());
            assertNotNull(dto.getPublisher());
            assertNotNull(dto.getImgUrls());
        }

        @Test
        @DisplayName("goodsId 小于 1 返回 400")
        void detailInvalidId() {
            Result result = controller.detail(0L);
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("商品不存在返回 404")
        void detailNotFound() {
            Result result = controller.detail(999999999L);
            assertEquals(404, result.getCode());
        }
    }

    // ======================== PUT /admin/goods/audit/approve ========================

    @Nested
    @DisplayName("PUT /admin/goods/audit/approve — 审核通过")
    class ApproveTests {

        @Test
        @DisplayName("通过待巡检商品后 isAudited=1 且重新上线")
        void approvePendingGoods() {
            GoodsAuditApproveRequest req = new GoodsAuditApproveRequest();
            req.setGoodsIds(Collections.singletonList(pending1Id));
            Result result = controller.approve(req, bind(req));
            assertEquals(200, result.getCode());

            TGoods goods = goodsService.getById(pending1Id);
            assertEquals(1, goods.getIsAudited());
            assertEquals(0, goods.getGoodsStatus());
        }

        @Test
        @DisplayName("goodsIds 为空返回 400")
        void approveEmptyIds() {
            GoodsAuditApproveRequest req = new GoodsAuditApproveRequest();
            req.setGoodsIds(Collections.emptyList());
            Result result = controller.approve(req, bind(req));
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("重复通过已通过商品返回 400")
        void approveAlreadyApproved() {
            GoodsAuditApproveRequest req = new GoodsAuditApproveRequest();
            req.setGoodsIds(Collections.singletonList(approvedId));
            Result result = controller.approve(req, bind(req));
            assertEquals(400, result.getCode());
            assertTrue(result.getMsg().contains("不在待巡检状态"));
        }

        @Test
        @DisplayName("商品不存在返回 404")
        void approveNotFound() {
            GoodsAuditApproveRequest req = new GoodsAuditApproveRequest();
            req.setGoodsIds(Collections.singletonList(999999999L));
            Result result = controller.approve(req, bind(req));
            assertEquals(404, result.getCode());
        }
    }

    // ======================== PUT /admin/goods/audit/reject ========================

    @Nested
    @DisplayName("PUT /admin/goods/audit/reject — 驳回下架")
    class RejectTests {

        @Test
        @DisplayName("驳回待巡检商品后 isAudited=2、下架并记录原因")
        void rejectPendingGoods() {
            GoodsAuditRejectRequest req = new GoodsAuditRejectRequest();
            req.setGoodsIds(Collections.singletonList(pending2Id));
            req.setReason("违反平台规定");
            Result result = controller.reject(req, bind(req));
            assertEquals(200, result.getCode());

            TGoods goods = goodsService.getById(pending2Id);
            assertEquals(2, goods.getIsAudited());
            assertEquals(2, goods.getGoodsStatus());
            assertEquals("违反平台规定", goods.getRejectReason());
        }

        @Test
        @DisplayName("reason 为空返回 400")
        void rejectMissingReason() {
            GoodsAuditRejectRequest req = new GoodsAuditRejectRequest();
            req.setGoodsIds(Collections.singletonList(pending1Id));
            req.setReason("");
            Result result = controller.reject(req, bind(req));
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("goodsIds 为空返回 400")
        void rejectEmptyIds() {
            GoodsAuditRejectRequest req = new GoodsAuditRejectRequest();
            req.setGoodsIds(Collections.emptyList());
            req.setReason("违规");
            Result result = controller.reject(req, bind(req));
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("重复驳回已驳回商品返回 400")
        void rejectAlreadyRejected() {
            GoodsAuditRejectRequest req = new GoodsAuditRejectRequest();
            req.setGoodsIds(Collections.singletonList(rejectedId));
            req.setReason("再次驳回");
            Result result = controller.reject(req, bind(req));
            assertEquals(400, result.getCode());
            assertTrue(result.getMsg().contains("不在待巡检状态"));
        }
    }
}
