package com.bit.scmu_taotao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.*;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.TAccountAuditLogMapper;
import com.bit.scmu_taotao.service.TFeedbackService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.service.TUserReportService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.common.Result;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("解决事项总台集成测试")
class AdminSolvedItemsControllerTest {

    @Autowired
    private AdminSolvedItemsController controller;

    @Autowired
    private TGoodsService goodsService;

    @Autowired
    private TUserService userService;

    @Autowired
    private TFeedbackService feedbackService;

    @Autowired
    private TUserReportService reportService;

    @Autowired
    private TAccountAuditLogMapper auditLogMapper;

    private static final String PREFIX = "ut_solved";
    private static final String PUBLISHER = PREFIX + "_pub";
    private static final String FB_USER = PREFIX + "_fb";
    private static final String BAN_USER = PREFIX + "_ban";
    private static final String CLEAR_USER = PREFIX + "_clr";
    private static final String REPORTER = PREFIX + "_reporter";

    private Long approvedGoodsId;
    private Long rejectedGoodsId;
    private Long pendingGoodsId;
    private Long processedFeedbackId;
    private Long pendingFeedbackId;
    private Long banAuditLogId;
    private Long clearAuditLogId;

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(PUBLISHER, "商品发布者", 100, 0);
        ensureUser(FB_USER, "反馈用户", 100, 0);
        ensureUser(BAN_USER, "被查封用户", 50, 1);
        ensureUser(CLEAR_USER, "被解除用户", 80, 0);
        ensureUser(REPORTER, "举报人", 100, 0);

        approvedGoodsId = insertGoods(PUBLISHER, "已通过商品", 1);
        rejectedGoodsId = insertGoods(PUBLISHER, "已打回商品", 2);
        pendingGoodsId = insertGoods(PUBLISHER, "待巡检商品", 0);

        TGoods rejectedGoods = goodsService.getById(rejectedGoodsId);
        rejectedGoods.setRejectReason("发布了敏感违禁信息");
        goodsService.updateById(rejectedGoods);

        processedFeedbackId = insertFeedback(FB_USER, "登录页面加载慢", 1, "已修复，请刷新后重试");
        pendingFeedbackId = insertFeedback(FB_USER, "希望增加夜间模式", 0, null);

        banAuditLogId = insertAuditLog(BAN_USER, "ban", 2, "信誉过低，多次违规");
        clearAuditLogId = insertAuditLog(CLEAR_USER, "clear", 1, "信用分已恢复");

        insertReport(REPORTER, BAN_USER, "信誉过低", 1);
        insertReport(REPORTER, BAN_USER, "信誉过低", 1);
        insertReport(REPORTER, BAN_USER, "商品违规", 1);
        insertReport(REPORTER, CLEAR_USER, "语言违规", 1);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        auditLogMapper.delete(new LambdaQueryWrapper<TAccountAuditLog>()
                .likeRight(TAccountAuditLog::getUserId, PREFIX + "_"));
        reportService.remove(new LambdaQueryWrapper<TUserReport>()
                .likeRight(TUserReport::getTargetId, PREFIX + "_"));
        feedbackService.remove(new LambdaQueryWrapper<TFeedback>()
                .likeRight(TFeedback::getUserId, PREFIX + "_"));
        goodsService.remove(new LambdaQueryWrapper<TGoods>()
                .likeRight(TGoods::getUserId, PREFIX + "_"));
        userService.remove(new LambdaQueryWrapper<TUser>()
                .likeRight(TUser::getUserId, PREFIX + "_"));
    }

    // ======================== 辅助方法 ========================

    private void ensureUser(String userId, String userName, int creditScore, int status) {
        if (userService.getById(userId) != null) return;
        TUser u = new TUser();
        u.setUserId(userId);
        u.setUserName(userName);
        u.setAvatar("https://example.com/avatar.png");
        u.setCreditScore(creditScore);
        u.setCreditStar(BigDecimal.valueOf(4.0));
        u.setStatus(status);
        u.setIsDelete(0);
        userService.save(u);
    }

    private Long insertGoods(String userId, String name, int isAudited) {
        TGoods g = new TGoods();
        g.setUserId(userId);
        g.setGoodsName(name);
        g.setGoodsDesc("测试商品描述");
        g.setGoodsNote("测试备注");
        g.setCategoryId(1);
        g.setGoodsType(0);
        g.setPrice(BigDecimal.valueOf(99.9));
        g.setUseScene("学习");
        g.setExchangePlace("图书馆");
        g.setGoodsStatus(0);
        g.setIsAudited(isAudited);
        g.setViewCount(0);
        g.setIsDelete(0);
        goodsService.save(g);
        return g.getGoodsId();
    }

    private Long insertFeedback(String userId, String content, int status, String replyContent) {
        TFeedback f = new TFeedback();
        f.setUserId(userId);
        f.setFeedbackContent(content);
        f.setFeedbackStatus(status);
        f.setIsRead(0);
        f.setIsDelete(0);
        if (replyContent != null) {
            f.setReplyContent(replyContent);
            f.setReplyTime(LocalDateTime.now().minusDays(1));
        }
        feedbackService.save(f);
        return f.getFeedbackId();
    }

    private Long insertAuditLog(String userId, String action, int previousStatus, String reason) {
        TAccountAuditLog log = new TAccountAuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setPreviousStatus(previousStatus);
        log.setReason(reason);
        log.setIsDelete(0);
        auditLogMapper.insert(log);
        return log.getId();
    }

    private void insertReport(String reporterId, String targetId, String tag, int status) {
        TUserReport r = new TUserReport();
        r.setReporterId(reporterId);
        r.setTargetId(targetId);
        r.setTag(tag);
        r.setContent("测试举报内容");
        r.setStatus(status);
        reportService.save(r);
    }

    private BindingResult bind(Object target) {
        return new BeanPropertyBindingResult(target, "request");
    }

    // ======================== 列表 ========================

    @Nested
    @DisplayName("GET /admin/solved-items/list — 解决事项列表")
    class ListTests {

        @Test
        @DisplayName("type=goods 返回已审核商品（isAudited IN 1,2）")
        void listGoodsTab() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("goods");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertNotNull(data);
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> list = (List<SolvedGoodsItemDTO>) data.get("list");
            assertNotNull(list);
            assertFalse(list.isEmpty(), "已审核商品列表不应为空");
            assertTrue(list.stream().anyMatch(i -> i.getGoodsId().equals(approvedGoodsId)));
            assertTrue(list.stream().anyMatch(i -> i.getGoodsId().equals(rejectedGoodsId)));
            assertFalse(list.stream().anyMatch(i -> i.getGoodsId().equals(pendingGoodsId)),
                    "待巡检商品不应出现在已解决列表中");
        }

        @Test
        @DisplayName("type=goods status=APPROVED 仅返回已通过商品")
        void listGoodsFilteredApproved() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("goods");
            req.setStatus("APPROVED");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> list = (List<SolvedGoodsItemDTO>) data.get("list");
            assertTrue(list.stream().allMatch(i -> "APPROVED".equals(i.getHandleStatus())));
            assertTrue(list.stream().anyMatch(i -> i.getGoodsId().equals(approvedGoodsId)));
        }

        @Test
        @DisplayName("type=goods status=REJECTED 仅返回已打回商品")
        void listGoodsFilteredRejected() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("goods");
            req.setStatus("REJECTED");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> list = (List<SolvedGoodsItemDTO>) data.get("list");
            assertTrue(list.stream().allMatch(i -> "REJECTED".equals(i.getHandleStatus())));
            assertTrue(list.stream().anyMatch(i -> i.getGoodsId().equals(rejectedGoodsId)));
        }

        @Test
        @DisplayName("type=goods 列表项包含必要字段")
        void listGoodsItemFields() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("goods");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> list = (List<SolvedGoodsItemDTO>) data.get("list");
            SolvedGoodsItemDTO item = list.stream()
                    .filter(i -> i.getGoodsId().equals(approvedGoodsId)).findFirst().orElse(null);
            assertNotNull(item, "应找到已通过商品");
            assertEquals("已通过商品", item.getName());
            assertNotNull(item.getImgUrl(), "imgUrl 不应为空");
            assertNotNull(item.getPublishTime(), "publishTime 不应为空");
            assertNotNull(item.getHandleTime(), "handleTime 不应为空");
        }

        @Test
        @DisplayName("type=feedback 返回已处理反馈")
        void listFeedbackTab() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("feedback");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedFeedbackItemDTO> list = (List<SolvedFeedbackItemDTO>) data.get("list");
            assertNotNull(list);
            assertFalse(list.isEmpty(), "已处理反馈列表不应为空");
            assertTrue(list.stream().anyMatch(i -> i.getFeedbackId().equals(processedFeedbackId)));
            assertFalse(list.stream().anyMatch(i -> i.getFeedbackId().equals(pendingFeedbackId)),
                    "待处理反馈不应出现在已解决列表中");
        }

        @Test
        @DisplayName("type=feedback 列表项包含回复内容和时间")
        void listFeedbackItemFields() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("feedback");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedFeedbackItemDTO> list = (List<SolvedFeedbackItemDTO>) data.get("list");
            SolvedFeedbackItemDTO item = list.stream()
                    .filter(i -> i.getFeedbackId().equals(processedFeedbackId)).findFirst().orElse(null);
            assertNotNull(item, "应找到已处理反馈");
            assertEquals(FB_USER, item.getUserId());
            assertEquals("反馈用户", item.getUserName());
            assertNotNull(item.getAvatar());
            assertEquals("已修复，请刷新后重试", item.getReplyContent());
            assertNotNull(item.getSubmitTime());
            assertNotNull(item.getReplyTime());
        }

        @Test
        @DisplayName("type=user 返回已处理账号审核记录")
        void listUserTab() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("user");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedUserItemDTO> list = (List<SolvedUserItemDTO>) data.get("list");
            assertNotNull(list);
            assertFalse(list.isEmpty(), "账号审核列表不应为空");
            assertTrue(list.stream().anyMatch(i -> BAN_USER.equals(i.getUserId())));
            assertTrue(list.stream().anyMatch(i -> CLEAR_USER.equals(i.getUserId())));
        }

        @Test
        @DisplayName("type=user status=BAN 仅返回查封记录")
        void listUserFilteredBan() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("user");
            req.setStatus("BAN");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedUserItemDTO> list = (List<SolvedUserItemDTO>) data.get("list");
            assertTrue(list.stream().allMatch(i -> "BAN".equals(i.getHandleStatus())));
            assertTrue(list.stream().anyMatch(i -> BAN_USER.equals(i.getUserId())));
        }

        @Test
        @DisplayName("type=user status=CLEAR 仅返回解除记录")
        void listUserFilteredClear() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("user");
            req.setStatus("CLEAR");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedUserItemDTO> list = (List<SolvedUserItemDTO>) data.get("list");
            assertTrue(list.stream().allMatch(i -> "CLEAR".equals(i.getHandleStatus())));
            assertTrue(list.stream().anyMatch(i -> CLEAR_USER.equals(i.getUserId())));
        }

        @Test
        @DisplayName("type=user reason 字段取已查实举报中数量最多的 tag")
        void listUserReasonIsMostFrequentTag() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("user");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedUserItemDTO> list = (List<SolvedUserItemDTO>) data.get("list");
            SolvedUserItemDTO banItem = list.stream()
                    .filter(i -> BAN_USER.equals(i.getUserId())).findFirst().orElse(null);
            assertNotNull(banItem);
            assertEquals("信誉过低", banItem.getReason());
        }

        @Test
        @DisplayName("type=user 列表项包含注册时间")
        void listUserItemFields() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("user");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedUserItemDTO> list = (List<SolvedUserItemDTO>) data.get("list");
            SolvedUserItemDTO item = list.get(0);
            assertNotNull(item.getUserId());
            assertNotNull(item.getUserName());
            assertNotNull(item.getAvatar());
            assertNotNull(item.getRegisterTime(), "registerTime 不应为空");
            assertNotNull(item.getHandleStatus());
        }

        @Test
        @DisplayName("keyword 搜索匹配商品名称")
        void listKeywordSearchGoods() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("goods");
            req.setKeyword("已通过商品");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> list = (List<SolvedGoodsItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getGoodsId().equals(approvedGoodsId)));
        }

        @Test
        @DisplayName("keyword 搜索匹配反馈内容")
        void listKeywordSearchFeedback() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("feedback");
            req.setKeyword("登录页面");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedFeedbackItemDTO> list = (List<SolvedFeedbackItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> i.getFeedbackId().equals(processedFeedbackId)));
        }

        @Test
        @DisplayName("keyword 搜索匹配用户名（账号审核）")
        void listKeywordSearchUser() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("user");
            req.setKeyword("被查封");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedUserItemDTO> list = (List<SolvedUserItemDTO>) data.get("list");
            assertTrue(list.stream().anyMatch(i -> BAN_USER.equals(i.getUserId())));
        }

        @Test
        @DisplayName("分页：page=1 size=1 返回正确子集")
        void listPagination() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType("goods");
            req.setPage(1);
            req.setSize(1);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> list = (List<SolvedGoodsItemDTO>) data.get("list");
            assertEquals(1, list.size(), "每页应返回 1 条");
            Long total = (Long) data.get("total");
            assertTrue(total >= 2L, "已审核商品总数应 >= 2");
//            assertEquals(2L, total.longValue(), "已审核商品总数应为 2");
        }

        @Test
        @DisplayName("type 缺失返回 400")
        void listMissingType() {
            SolvedItemListRequest req = new SolvedItemListRequest();
            req.setType(null);
            req.setPage(1);
            req.setSize(10);
            BindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("type", "NotBlank", "type不能为空");
            Result result = controller.list(req, br);
            assertEquals(400, result.getCode());
        }
    }

    // ======================== 详情 ========================

    @Nested
    @DisplayName("GET /admin/solved-items/detail — 解决事项详情")
    class DetailTests {

        @Test
        @DisplayName("type=goods 返回完整商品详情")
        void detailGoods() {
            Result result = controller.detail("goods", String.valueOf(approvedGoodsId));
            assertEquals(200, result.getCode());

            SolvedItemDetailResponse resp = (SolvedItemDetailResponse) result.getData();
            assertEquals("goods", resp.getType());
            assertEquals("APPROVED", resp.getHandleStatus());
            assertNotNull(resp.getHandleTime());

            assertNotNull(resp.getGoodsDetail(), "goodsDetail 不应为 null");
            assertNull(resp.getFeedbackDetail());
            assertNull(resp.getUserDetail());

            SolvedGoodsDetailDTO gd = resp.getGoodsDetail();
            assertEquals(approvedGoodsId, gd.getGoodsId());
            assertEquals("已通过商品", gd.getName());
            assertNotNull(gd.getDesc());
            assertEquals("测试备注", gd.getRemark());
            assertEquals(0, gd.getPrice().compareTo(BigDecimal.valueOf(99.9)));
            assertNotNull(gd.getPurpose());
            assertNotNull(gd.getExchangeAddr());
            assertNotNull(gd.getImgUrls());
            assertNotNull(gd.getPublishTime());
            assertNotNull(gd.getGoodsType());
            assertNotNull(gd.getPublisher(), "publisher 不应为 null");
            assertEquals(PUBLISHER, gd.getPublisher().getUserId());
            assertNotNull(gd.getPublisher().getUserName());
            assertNotNull(gd.getPublisher().getCreditScore());
        }

        @Test
        @DisplayName("type=goods REJECTED 详情包含 rejectReason")
        void detailGoodsRejectedHasReason() {
            Result result = controller.detail("goods", String.valueOf(rejectedGoodsId));
            assertEquals(200, result.getCode());

            SolvedItemDetailResponse resp = (SolvedItemDetailResponse) result.getData();
            assertEquals("REJECTED", resp.getHandleStatus());
            assertNotNull(resp.getGoodsDetail().getRejectReason());
            assertTrue(resp.getGoodsDetail().getRejectReason().contains("敏感违禁"));
        }

        @Test
        @DisplayName("type=feedback 返回完整反馈详情")
        void detailFeedback() {
            Result result = controller.detail("feedback", String.valueOf(processedFeedbackId));
            assertEquals(200, result.getCode());

            SolvedItemDetailResponse resp = (SolvedItemDetailResponse) result.getData();
            assertEquals("feedback", resp.getType());
            assertEquals("PROCESSED", resp.getHandleStatus());
            assertNotNull(resp.getHandleTime());

            assertNotNull(resp.getFeedbackDetail(), "feedbackDetail 不应为 null");
            assertNull(resp.getGoodsDetail());
            assertNull(resp.getUserDetail());

            SolvedFeedbackDetailDTO fd = resp.getFeedbackDetail();
            assertEquals(processedFeedbackId, fd.getFeedbackId());
            assertEquals(FB_USER, fd.getUserId());
            assertEquals("反馈用户", fd.getUserName());
            assertNotNull(fd.getAvatar());
            assertNotNull(fd.getSubmitTime());
            assertEquals("登录页面加载慢", fd.getContent());
            assertEquals("已修复，请刷新后重试", fd.getReplyContent());
        }

        @Test
        @DisplayName("type=user 返回完整账号审核详情")
        void detailUser() {
            Result result = controller.detail("user", BAN_USER);
            assertEquals(200, result.getCode());

            SolvedItemDetailResponse resp = (SolvedItemDetailResponse) result.getData();
            assertEquals("user", resp.getType());
            assertEquals("BAN", resp.getHandleStatus());
            assertNotNull(resp.getHandleTime());

            assertNotNull(resp.getUserDetail(), "userDetail 不应为 null");
            assertNull(resp.getGoodsDetail());
            assertNull(resp.getFeedbackDetail());

            SolvedUserDetailDTO ud = resp.getUserDetail();
            assertEquals(BAN_USER, ud.getUserId());
            assertEquals("被查封用户", ud.getUserName());
            assertNotNull(ud.getAvatar());
            assertNotNull(ud.getCreditScore());
            assertNotNull(ud.getCreditStar());
            assertNotNull(ud.getReasonCategory(), "reasonCategory 不应为空");
            assertEquals("信誉过低", ud.getReasonCategory());
            assertNotNull(ud.getViolationReason());
        }

        @Test
        @DisplayName("type=user CLEAR 详情 handleStatus=CLEAR")
        void detailUserClear() {
            Result result = controller.detail("user", CLEAR_USER);
            assertEquals(200, result.getCode());

            SolvedItemDetailResponse resp = (SolvedItemDetailResponse) result.getData();
            assertEquals("CLEAR", resp.getHandleStatus());
            assertNotNull(resp.getUserDetail());
            assertEquals(CLEAR_USER, resp.getUserDetail().getUserId());
        }

        @Test
        @DisplayName("不存在的事项返回 400")
        void detailNotFound() {
            Result result = controller.detail("goods", "99999999");
            assertEquals(400, result.getCode());
        }
    }

    // ======================== 数量统计 ========================

    @Nested
    @DisplayName("GET /admin/solved-items/count — 数量统计")
    class CountTests {

        @Test
        @DisplayName("返回三种类型的已解决数量")
        void countReturnsAllTypes() {
            Result result = controller.count();
            assertEquals(200, result.getCode());

            SolvedItemCountDTO dto = (SolvedItemCountDTO) result.getData();
            assertTrue(dto.getGoodsCount() >= 2, "商品已解决数量应 >= 2");
            assertTrue(dto.getFeedbackCount() >= 1, "反馈已解决数量应 >= 1");
            assertTrue(dto.getUserCount() >= 2, "账号审核已解决数量应 >= 2");
            assertEquals(dto.getGoodsCount() + dto.getFeedbackCount() + dto.getUserCount(),
                    dto.getTotalCount(), "totalCount 应为三者之和");
        }
    }

    // ======================== 撤销 ========================

    @Nested
    @DisplayName("PUT /admin/solved-items/revoke — 撤销操作")
    class RevokeTests {

        @Test
        @DisplayName("撤销已通过商品：isAudited 恢复为 0")
        void revokeGoodsApproved() {
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("goods");
            req.setId(String.valueOf(approvedGoodsId));
            Result result = controller.revoke(req, bind(req));

            assertEquals(200, result.getCode());
            TGoods goods = goodsService.getById(approvedGoodsId);
            assertNotNull(goods);
            assertEquals(0, goods.getIsAudited(), "isAudited 应回到 0");
        }

        @Test
        @DisplayName("撤销已打回商品：isAudited 恢复为 0，rejectReason 清空")
        void revokeGoodsRejected() {
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("goods");
            req.setId(String.valueOf(rejectedGoodsId));
            Result result = controller.revoke(req, bind(req));

            assertEquals(200, result.getCode());
            TGoods goods = goodsService.getById(rejectedGoodsId);
            assertNotNull(goods);
            assertEquals(0, goods.getIsAudited());
            assertNull(goods.getRejectReason(), "rejectReason 应被清空");
        }

        @Test
        @DisplayName("撤销已处理反馈：feedbackStatus 恢复为 0")
        void revokeFeedback() {
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("feedback");
            req.setId(String.valueOf(processedFeedbackId));
            Result result = controller.revoke(req, bind(req));

            assertEquals(200, result.getCode());
            TFeedback fb = feedbackService.getById(processedFeedbackId);
            assertNotNull(fb);
            assertEquals(0, fb.getFeedbackStatus(), "feedbackStatus 应回到 0");
        }

        @Test
        @DisplayName("撤销 BAN：TUser.status 恢复为 previous_status，审计日志软删")
        void revokeUserBan() {
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("user");
            req.setId(BAN_USER);
            Result result = controller.revoke(req, bind(req));

            assertEquals(200, result.getCode());

            TUser user = userService.getById(BAN_USER);
            assertNotNull(user);
            assertEquals(2, user.getStatus(), "status 应回到 previous_status=2");

            TAccountAuditLog log = auditLogMapper.selectById(banAuditLogId);
            assertNotNull(log, "记录不应被物理删除");
            assertEquals(1, log.getIsDelete(), "审计日志应被软删");
        }

        @Test
        @DisplayName("撤销 CLEAR：TUser.status 恢复为 previous_status，审计日志软删")
        void revokeUserClear() {
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("user");
            req.setId(CLEAR_USER);
            Result result = controller.revoke(req, bind(req));

            assertEquals(200, result.getCode());

            TUser user = userService.getById(CLEAR_USER);
            assertNotNull(user);
            assertEquals(1, user.getStatus(), "status 应回到 previous_status=1");

            TAccountAuditLog log = auditLogMapper.selectById(clearAuditLogId);
            assertNotNull(log);
            assertEquals(1, log.getIsDelete());
        }

        @Test
        @DisplayName("撤销不存在的事项返回 400")
        void revokeNotFound() {
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("goods");
            req.setId("99999999");
            Result result = controller.revoke(req, bind(req));

            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("request 为空返回 400")
        void revokeNullBody() {
            Result result = controller.revoke(null, null);
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("撤销成功后该事项不再出现在已解决列表中")
        void revokeDisappearsFromList() {
            // 先确认在列表中
            SolvedItemListRequest listReq = new SolvedItemListRequest();
            listReq.setType("goods");
            listReq.setPage(1);
            listReq.setSize(50);
            Result beforeResult = controller.list(listReq, bind(listReq));
            @SuppressWarnings("unchecked")
            Map<String, Object> beforeData = (Map<String, Object>) beforeResult.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> beforeList = (List<SolvedGoodsItemDTO>) beforeData.get("list");
            assertTrue(beforeList.stream().anyMatch(i -> i.getGoodsId().equals(approvedGoodsId)));

            // 撤销
            SolvedItemRevokeRequest req = new SolvedItemRevokeRequest();
            req.setType("goods");
            req.setId(String.valueOf(approvedGoodsId));
            controller.revoke(req, bind(req));

            // 再查列表，不应出现
            Result afterResult = controller.list(listReq, bind(listReq));
            @SuppressWarnings("unchecked")
            Map<String, Object> afterData = (Map<String, Object>) afterResult.getData();
            @SuppressWarnings("unchecked")
            List<SolvedGoodsItemDTO> afterList = (List<SolvedGoodsItemDTO>) afterData.get("list");
            assertFalse(afterList.stream().anyMatch(i -> i.getGoodsId().equals(approvedGoodsId)),
                    "撤销后商品不应再出现在已解决列表中");
        }
    }
}
