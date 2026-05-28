package com.bit.scmu_taotao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.*;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.service.*;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("管理员风险账号审核集成测试")
class AdminRiskUserControllerTest {

    @Autowired
    private AdminRiskUserController controller;

    @Autowired
    private AdminRiskUserService riskUserService;

    @Autowired
    private TUserService userService;

    @Autowired
    private TUserReportService reportService;

    @Autowired
    private TGoodsService goodsService;

    @Autowired
    private TBlacklistService blacklistService;

    @Autowired
    private TEvaluateService evaluateService;

    @Autowired
    private TTradeService tradeService;

    @Autowired
    private TCreditLogService creditLogService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @MockBean
    private StompPushService stompPushService;

    private static final String PREFIX = "ut_risk";
    private static final String RISK_USER = PREFIX + "_risk01";
    private static final String NORMAL_USER = PREFIX + "_normal";

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(NORMAL_USER, "正常用户", 100, 0);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        creditLogService.remove(new LambdaQueryWrapper<TCreditLog>()
                .likeRight(TCreditLog::getUserId, PREFIX + "_"));
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSendId, "system")
                .likeRight(ChatMessage::getReceiveId, PREFIX + "_"));
        evaluateService.remove(new LambdaQueryWrapper<TEvaluate>()
                .likeRight(TEvaluate::getSellerId, PREFIX + "_"));
        tradeService.remove(new LambdaQueryWrapper<com.bit.scmu_taotao.entity.TTrade>()
                .likeRight(com.bit.scmu_taotao.entity.TTrade::getSellerId, PREFIX + "_"));
        blacklistService.remove(new LambdaQueryWrapper<TBlacklist>()
                .likeRight(TBlacklist::getBlackUserId, PREFIX + "_"));
        goodsService.remove(new LambdaQueryWrapper<TGoods>()
                .likeRight(TGoods::getUserId, PREFIX + "_"));
        reportService.remove(new LambdaQueryWrapper<TUserReport>()
                .likeRight(TUserReport::getTargetId, PREFIX + "_"));
        userService.remove(new LambdaQueryWrapper<TUser>()
                .likeRight(TUser::getUserId, PREFIX + "_"));
    }

    private void ensureUser(String userId, String userName, int creditScore, int status) {
        if (userService.getById(userId) != null) return;
        TUser u = new TUser();
        u.setUserId(userId);
        u.setUserName(userName);
        u.setAvatar("https://example.com/avatar.png");
        u.setCreditScore(creditScore);
        u.setCreditStar(BigDecimal.valueOf(5.0));
        u.setStatus(status);
        u.setIsDelete(0);
        userService.save(u);
    }

    private void updateUserCreditScore(String userId, int score) {
        TUser u = userService.getById(userId);
        assertNotNull(u, "用户不存在: " + userId);
        u.setCreditScore(score);
        userService.updateById(u);
    }

    private void updateUserStatus(String userId, int status) {
        TUser u = userService.getById(userId);
        assertNotNull(u, "用户不存在: " + userId);
        u.setStatus(status);
        userService.updateById(u);
    }

    private BindingResult bind(Object target) {
        return new BeanPropertyBindingResult(target, "request");
    }

    // ======================== 列表 ========================

    @Nested
    @DisplayName("GET /admin/users/risk/list — 风险用户列表")
    class ListTests {

        @Test
        @DisplayName("只返回 creditScore < 70 且 status=2 的用户")
        void listOnlyRiskUsers() {
            ensureUser(PREFIX + "_r1", "风险用户A", 65, 2);
            ensureUser(PREFIX + "_r2", "风险用户B", 50, 2);
            // creditScore >= 70，不应出现
            ensureUser(PREFIX + "_n1", "正常用户C", 80, 0);
            // creditScore < 70 但 status=0，不应出现
            ensureUser(PREFIX + "_n2", "正常用户D", 60, 0);
            // creditScore < 70 但 status=1（已查封），不应出现
            ensureUser(PREFIX + "_n3", "已查封用户", 30, 1);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            assertEquals(2, list.size());
            assertTrue(list.stream().anyMatch(dto -> dto.getUserId().equals(PREFIX + "_r1")));
            assertTrue(list.stream().anyMatch(dto -> dto.getUserId().equals(PREFIX + "_r2")));
        }

        @Test
        @DisplayName("风险等级计算：低度风险 60-69")
        void listRiskLevelLow() {
            ensureUser(PREFIX + "_low", "低度风险用户", 65, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            RiskUserListItemDTO dto = list.stream()
                    .filter(d -> d.getUserId().equals(PREFIX + "_low"))
                    .findFirst().orElse(null);
            assertNotNull(dto);
            assertEquals("低度风险", dto.getRiskLevel());
        }

        @Test
        @DisplayName("风险等级计算：中度风险 40-59")
        void listRiskLevelMedium() {
            ensureUser(PREFIX + "_mid", "中度风险用户", 45, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            RiskUserListItemDTO dto = list.stream()
                    .filter(d -> d.getUserId().equals(PREFIX + "_mid"))
                    .findFirst().orElse(null);
            assertNotNull(dto);
            assertEquals("中度风险", dto.getRiskLevel());
        }

        @Test
        @DisplayName("风险等级计算：高度风险 0-39")
        void listRiskLevelHigh() {
            ensureUser(PREFIX + "_high", "高度风险用户", 20, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            RiskUserListItemDTO dto = list.stream()
                    .filter(d -> d.getUserId().equals(PREFIX + "_high"))
                    .findFirst().orElse(null);
            assertNotNull(dto);
            assertEquals("高度风险", dto.getRiskLevel());
        }

        @Test
        @DisplayName("关键词搜索：按姓名匹配")
        void listSearchByName() {
            ensureUser(PREFIX + "_s1", "张三丰", 60, 2);
            ensureUser(PREFIX + "_s2", "李四", 55, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setKeyword("张三");
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            assertEquals(1, list.size());
            assertEquals(PREFIX + "_s1", list.get(0).getUserId());
        }

        @Test
        @DisplayName("关键词搜索：按学号匹配")
        void listSearchByUserId() {
            ensureUser(PREFIX + "_s3", "王五", 60, 2);
            ensureUser(PREFIX + "_s4", "赵六", 55, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setKeyword(PREFIX + "_s3");
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            assertEquals(1, list.size());
            assertEquals(PREFIX + "_s3", list.get(0).getUserId());
        }

        @Test
        @DisplayName("无匹配关键词时返回空列表")
        void listSearchNoMatch() {
            ensureUser(PREFIX + "_s5", "钱七", 60, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setKeyword("不存在的关键词");
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            assertEquals(0, list.size());
        }

        @Test
        @DisplayName("返回空列表当无风险用户")
        void listEmpty() {
            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            assertEquals(0, list.size());
        }

        @Test
        @DisplayName("分页功能正常")
        void listPagination() {
            for (int i = 0; i < 5; i++) {
                ensureUser(PREFIX + "_p" + i, "分页用户" + i, 60, 2);
            }

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(2);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            assertEquals(2, list.size());
            assertTrue(result.getTotal() >= 5);
        }

        @Test
        @DisplayName("返回的 DTO 包含所有必要字段")
        void listDtoFields() {
            ensureUser(PREFIX + "_f1", "字段测试用户", 65, 2);

            RiskUserPageRequest req = new RiskUserPageRequest();
            req.setPage(1);
            req.setPageSize(50);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<RiskUserListItemDTO> list = (List<RiskUserListItemDTO>) result.getData();
            RiskUserListItemDTO dto = list.stream()
                    .filter(d -> d.getUserId().equals(PREFIX + "_f1"))
                    .findFirst().orElse(null);
            assertNotNull(dto);
            assertNotNull(dto.getUserId());
            assertNotNull(dto.getUserName());
            assertNotNull(dto.getAvatar());
            assertNotNull(dto.getRiskLevel());
            assertNotNull(dto.getRegisterTime());
        }
    }

    // ======================== 多维详情 ========================

    @Nested
    @DisplayName("GET /admin/users/risk/{userId}/metrics — 风险多维详情")
    class MetricsTests {

        @Test
        @DisplayName("返回用户基础信息和五维指标")
        void metricsBasic() {
            ensureUser(RISK_USER, "风险用户", 60, 2);

            // 插入 2 条已核实举报
            insertReport(RISK_USER, "GOODS_VIOLATION", 1);
            insertReport(RISK_USER, "LANG_VIOLATION", 1);
            // 插入 1 条未核实举报（不应计入）
            insertReport(RISK_USER, "OTHER", 0);

            // 插入 1 条违规商品
            insertGoods(RISK_USER, 2);

            // 插入 2 次被拉黑
            ensureUser(PREFIX + "_bl_a", "拉黑者A", 100, 0);
            ensureUser(PREFIX + "_bl_b", "拉黑者B", 100, 0);
            insertBlacklist(RISK_USER, PREFIX + "_bl_a");
            insertBlacklist(RISK_USER, PREFIX + "_bl_b");

            // 插入 1 条低分评价
            insertEvaluate(RISK_USER, 30);

            Result result = controller.metrics(RISK_USER);

            assertEquals(200, result.getCode());
            RiskMetricsDTO data = (RiskMetricsDTO) result.getData();
            assertEquals(RISK_USER, data.getUserId());
            assertEquals("风险用户", data.getUserName());
            assertEquals(60, data.getCreditScore());
            assertNotNull(data.getCreditStar());

            RiskMetricsDTO.Metrics metrics = data.getMetrics();
            assertNotNull(metrics);
            assertEquals(2L, metrics.getReportCount());
            assertEquals(1L, metrics.getItemViolationCount());
            assertEquals(2L, metrics.getBlacklistCount());
            assertEquals(1L, metrics.getLangViolationCount());
            assertEquals(1L, metrics.getLowRatingCount());
        }

        @Test
        @DisplayName("用户不存在返回 404")
        void metricsUserNotFound() {
            Result result = controller.metrics(PREFIX + "_nonexistent");
            assertEquals(404, result.getCode());
        }

        @Test
        @DisplayName("已删除用户返回 404")
        void metricsDeletedUser() {
            ensureUser(PREFIX + "_deleted", "已删除用户", 60, 2);
            TUser u = userService.getById(PREFIX + "_deleted");
            u.setIsDelete(1);
            userService.updateById(u);

            Result result = controller.metrics(PREFIX + "_deleted");
            assertEquals(404, result.getCode());
        }

        @Test
        @DisplayName("无任何违规记录时指标全为 0")
        void metricsAllZero() {
            ensureUser(PREFIX + "_clean", "干净用户", 60, 2);

            Result result = controller.metrics(PREFIX + "_clean");

            assertEquals(200, result.getCode());
            RiskMetricsDTO data = (RiskMetricsDTO) result.getData();
            RiskMetricsDTO.Metrics metrics = data.getMetrics();
            assertEquals(0L, metrics.getReportCount());
            assertEquals(0L, metrics.getItemViolationCount());
            assertEquals(0L, metrics.getBlacklistCount());
            assertEquals(0L, metrics.getLangViolationCount());
            assertEquals(0L, metrics.getLowRatingCount());
        }

        @Test
        @DisplayName("低分评价只统计 sellerId 匹配且 totalScore < 40")
        void metricsLowRatingFilter() {
            ensureUser(PREFIX + "_lr1", "评价测试用户", 60, 2);
            // totalScore < 40，应计入
            insertEvaluate(PREFIX + "_lr1", 35);
            // totalScore = 40，不应计入（边界值）
            insertEvaluate(PREFIX + "_lr1", 40);
            // totalScore > 40，不应计入
            insertEvaluate(PREFIX + "_lr1", 50);

            Result result = controller.metrics(PREFIX + "_lr1");

            assertEquals(200, result.getCode());
            RiskMetricsDTO data = (RiskMetricsDTO) result.getData();
            RiskMetricsDTO.Metrics metrics = data.getMetrics();
            assertEquals(1L, metrics.getLowRatingCount());
        }
    }

    // ======================== 查封/消除 ========================

    @Nested
    @DisplayName("PUT /admin/users/risk/handle — 查封/消除")
    class HandleTests {

        @Test
        @DisplayName("BAN 单个用户：status→1，记录 violationReason")
        void handleBanSingle() {
            ensureUser(PREFIX + "_ban1", "查封用户", 60, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_ban1"));
            req.setAction("BAN");
            req.setReason("多次违规发布");
            Result result = controller.handle(req, bind(req));

            assertEquals(200, result.getCode());
            assertTrue(result.getData().toString().contains("操作成功 1 条"));

            TUser updated = userService.getById(PREFIX + "_ban1");
            assertEquals(1, updated.getStatus());
            assertEquals("多次违规发布", updated.getViolationReason());
        }

        @Test
        @DisplayName("CLEAR 单个用户：status→0，creditScore→80，清空 violationReason")
        void handleClearSingle() {
            ensureUser(PREFIX + "_clr1", "消除用户", 50, 2);
            TUser u = userService.getById(PREFIX + "_clr1");
            u.setViolationReason("旧原因");
            userService.updateById(u);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_clr1"));
            req.setAction("CLEAR");
            req.setReason(null);
            Result result = controller.handle(req, bind(req));

            assertEquals(200, result.getCode());

            TUser updated = userService.getById(PREFIX + "_clr1");
            assertEquals(0, updated.getStatus());
            assertEquals(80, updated.getCreditScore());
            assertNull(updated.getViolationReason());
        }

        @Test
        @DisplayName("CLEAR 用户原分 > 80 时不降分")
        void handleClearHighScore() {
            ensureUser(PREFIX + "_clr2", "高分用户", 85, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_clr2"));
            req.setAction("CLEAR");
            Result result = controller.handle(req, bind(req));

            assertEquals(200, result.getCode());

            TUser updated = userService.getById(PREFIX + "_clr2");
            assertEquals(0, updated.getStatus());
            assertEquals(85, updated.getCreditScore());
        }

        @Test
        @DisplayName("批量 BAN：多个用户同时查封")
        void handleBatchBan() {
            ensureUser(PREFIX + "_bb1", "批量查封A", 60, 2);
            ensureUser(PREFIX + "_bb2", "批量查封B", 45, 2);
            ensureUser(PREFIX + "_bb3", "批量查封C", 30, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_bb1", PREFIX + "_bb2", PREFIX + "_bb3"));
            req.setAction("BAN");
            req.setReason("批量查封测试");
            Result result = controller.handle(req, bind(req));

            assertEquals(200, result.getCode());
            assertTrue(result.getData().toString().contains("操作成功 3 条"));

            for (String id : List.of(PREFIX + "_bb1", PREFIX + "_bb2", PREFIX + "_bb3")) {
                TUser u = userService.getById(id);
                assertEquals(1, u.getStatus());
                assertEquals("批量查封测试", u.getViolationReason());
            }
        }

        @Test
        @DisplayName("批量操作中跳过不存在的用户")
        void handleBatchSkipMissing() {
            ensureUser(PREFIX + "_bm1", "存在用户", 60, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_bm1", PREFIX + "_nonexist"));
            req.setAction("BAN");
            req.setReason("测试");
            Result result = controller.handle(req, bind(req));

            assertEquals(200, result.getCode());
            assertTrue(result.getData().toString().contains("操作成功 1 条"));

            TUser u = userService.getById(PREFIX + "_bm1");
            assertEquals(1, u.getStatus());
        }

        @Test
        @DisplayName("BAN 写入 TCreditLog（changeType=RISK_BAN）")
        void handleBanWritesCreditLog() {
            ensureUser(PREFIX + "_bl1", "日志测试用户", 60, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_bl1"));
            req.setAction("BAN");
            req.setReason("测试原因");
            controller.handle(req, bind(req));

            TCreditLog log = creditLogService.getOne(new LambdaQueryWrapper<TCreditLog>()
                    .eq(TCreditLog::getUserId, PREFIX + "_bl1")
                    .eq(TCreditLog::getChangeType, "RISK_BAN"));
            assertNotNull(log);
            assertEquals(0, log.getScoreChange());
            assertTrue(log.getReason().contains("测试原因"));
        }

        @Test
        @DisplayName("CLEAR 写入 TCreditLog（changeType=RISK_CLEAR）")
        void handleClearWritesCreditLog() {
            ensureUser(PREFIX + "_cl1", "日志测试用户", 50, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_cl1"));
            req.setAction("CLEAR");
            controller.handle(req, bind(req));

            TCreditLog log = creditLogService.getOne(new LambdaQueryWrapper<TCreditLog>()
                    .eq(TCreditLog::getUserId, PREFIX + "_cl1")
                    .eq(TCreditLog::getChangeType, "RISK_CLEAR"));
            assertNotNull(log);
            assertEquals(30, log.getScoreChange());
            assertTrue(log.getReason().contains("风险账号消除"));
        }

        @Test
        @DisplayName("BAN 后发送系统通知消息")
        void handleBanSendsNotification() {
            ensureUser(PREFIX + "_bn1", "通知测试用户", 60, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_bn1"));
            req.setAction("BAN");
            req.setReason("违规测试");
            controller.handle(req, bind(req));

            Mockito.verify(stompPushService).pushToUserQueue(
                    Mockito.eq(PREFIX + "_bn1"),
                    Mockito.eq("/queue/messages"),
                    Mockito.any());

            ChatMessage msg = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSendId, "system")
                    .eq(ChatMessage::getReceiveId, PREFIX + "_bn1"));
            assertNotNull(msg);
            assertTrue(msg.getMsgContent().contains("违规测试"));
        }

        @Test
        @DisplayName("CLEAR 后发送系统通知消息")
        void handleClearSendsNotification() {
            ensureUser(PREFIX + "_cn1", "通知测试用户", 50, 2);

            RiskHandleRequest req = new RiskHandleRequest();
            req.setUserIds(List.of(PREFIX + "_cn1"));
            req.setAction("CLEAR");
            controller.handle(req, bind(req));

            Mockito.verify(stompPushService).pushToUserQueue(
                    Mockito.eq(PREFIX + "_cn1"),
                    Mockito.eq("/queue/messages"),
                    Mockito.any());

            ChatMessage msg = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSendId, "system")
                    .eq(ChatMessage::getReceiveId, PREFIX + "_cn1"));
            assertNotNull(msg);
            assertTrue(msg.getMsgContent().contains("风险已解除"));
        }
    }

    // ======================== 辅助方法 ========================

    /** 插入举报记录 */
    private void insertReport(String targetId, String tag, int status) {
        TUserReport r = new TUserReport();
        r.setReporterId(NORMAL_USER);
        r.setTargetId(targetId);
        r.setTag(tag);
        r.setContent("测试举报内容");
        r.setStatus(status);
        reportService.save(r);
    }

    /** 插入违规商品 */
    private void insertGoods(String userId, int isAudited) {
        TGoods g = new TGoods();
        g.setUserId(userId);
        g.setGoodsName("测试商品");
        g.setCategoryId(1);
        g.setGoodsType(1);
        g.setPrice(BigDecimal.TEN);
        g.setGoodsStatus(0);
        g.setIsAudited(isAudited);
        g.setExchangePlace("测试地点");
        g.setIsDelete(0);
        goodsService.save(g);
    }

    /** 插入拉黑记录 */
    private void insertBlacklist(String targetUserId, String blackUserId) {
        TBlacklist b = new TBlacklist();
        b.setUserId(blackUserId);
        b.setBlackUserId(targetUserId);
        b.setIsDelete(0);
        blacklistService.save(b);
    }

    /** 插入交易记录（自动创建关联商品） */
    private Long insertTrade(String sellerId, String buyerId) {
        TGoods goods = new TGoods();
        goods.setUserId(sellerId);
        goods.setGoodsName("测试交易商品");
        goods.setCategoryId(1);
        goods.setGoodsType(1);
        goods.setPrice(java.math.BigDecimal.TEN);
        goods.setGoodsStatus(1);
        goods.setExchangePlace("测试地点");
        goods.setIsDelete(0);
        goodsService.save(goods);

        com.bit.scmu_taotao.entity.TTrade t = new com.bit.scmu_taotao.entity.TTrade();
        t.setGoodsId(goods.getGoodsId());
        t.setSellerId(sellerId);
        t.setBuyerId(buyerId);
        t.setTradePrice(java.math.BigDecimal.TEN);
        t.setIsDelete(0);
        tradeService.save(t);
        return t.getTradeId();
    }

    /** 插入低分评价 */
    private void insertEvaluate(String sellerId, int totalScore) {
        Long tradeId = insertTrade(sellerId, NORMAL_USER);
        TEvaluate e = new TEvaluate();
        e.setBuyerId(NORMAL_USER);
        e.setSellerId(sellerId);
        e.setTradeId(tradeId);
        e.setGoodsId(0L);
        e.setDescScore(totalScore);
        e.setCommScore(totalScore);
        e.setTotalScore(totalScore);
        e.setIsDelete(0);
        evaluateService.save(e);
    }
}







