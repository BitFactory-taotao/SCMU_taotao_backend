package com.bit.scmu_taotao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.AdminStatisticsOverviewDTO;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.TAccountAuditLogMapper;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.common.Result;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 概览工作台集成测试
 * 对应接口：GET /admin/statistics/overview
 *
 * 使用 @Transactional 保证每个测试方法的数据隔离（自动回滚）。
 * 断言采用"快照增量"方式，不依赖数据库初始状态。
 */
@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@Transactional
@DisplayName("概览工作台集成测试")
class AdminStatisticsControllerTest {

    @Autowired
    private AdminStatisticsController controller;

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

    /** 模拟 RedisService，避免测试依赖真实 Redis */
    @MockBean
    private RedisService redisService;

    private static final String PREFIX = "ut_dashboard";
    private static final String PUBLISHER = PREFIX + "_pub";
    private static final String FB_USER = PREFIX + "_fb";
    private static final String REPORTER = PREFIX + "_reporter";

    @BeforeEach
    void setUp() {
        cleanTestData();
        // hGetAll 返回空 Map，模拟 Redis 无历史快照（避免 NPE 和错误降级）
        Mockito.when(redisService.hGetAll(Mockito.anyString())).thenReturn(new HashMap<>());
        ensureUser(PUBLISHER, "商品发布者", 100, 0);
        ensureUser(FB_USER, "反馈用户", 100, 0);
        ensureUser(REPORTER, "举报人", 100, 0);
    }

    private void cleanTestData() {
        auditLogMapper.delete(new LambdaQueryWrapper<TAccountAuditLog>()
                .likeRight(TAccountAuditLog::getUserId, PREFIX + "_"));
        reportService.remove(new LambdaQueryWrapper<TUserReport>()
                .likeRight(TUserReport::getReporterId, PREFIX + "_"));
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

    /** 获取当前概览数据（用于快照对比） */
    private AdminStatisticsOverviewDTO fetchOverview() {
        Result result = controller.overview();
        assertEquals(200, result.getCode(), "接口应返回200: " + result.getMsg());
        assertNotNull(result.getData(), "data 不应为 null");
        return (AdminStatisticsOverviewDTO) result.getData();
    }

    private long getMetricCount(AdminStatisticsOverviewDTO dto, String key) {
        return getMetricByKey(dto, key).getCount();
    }

    private String getMetricTrend(AdminStatisticsOverviewDTO dto, String key) {
        return getMetricByKey(dto, key).getTrend();
    }

    private AdminStatisticsOverviewDTO.Metric getMetricByKey(AdminStatisticsOverviewDTO dto, String key) {
        AdminStatisticsOverviewDTO.Metric metric = switch (key) {
            case "pendingGoods" -> dto.getPendingGoods();
            case "riskUsers" -> dto.getRiskUsers();
            case "pendingFeedback" -> dto.getPendingFeedback();
            case "pendingReports" -> dto.getPendingReports();
            case "solvedItems" -> dto.getSolvedItems();
            default -> throw new IllegalArgumentException("未知指标: " + key);
        };
        assertNotNull(metric, key + " 不应为 null");
        return metric;
    }

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

    private void insertGoods(String userId, String name, int goodsStatus, int isAudited) {
        TGoods g = new TGoods();
        g.setUserId(userId);
        g.setGoodsName(name);
        g.setCategoryId(1);
        g.setGoodsType(1);
        g.setPrice(BigDecimal.TEN);
        g.setGoodsStatus(goodsStatus);
        g.setIsAudited(isAudited);
        g.setExchangePlace("测试地点");
        g.setIsDelete(0);
        goodsService.save(g);
    }

    private void insertFeedback(String userId, String content, int feedbackStatus) {
        TFeedback f = new TFeedback();
        f.setUserId(userId);
        f.setFeedbackContent(content);
        f.setFeedbackStatus(feedbackStatus);
        f.setIsRead(0);
        f.setIsDelete(0);
        feedbackService.save(f);
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

    private void insertAuditLog(String userId, String action, int previousStatus, String reason) {
        TAccountAuditLog log = new TAccountAuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setPreviousStatus(previousStatus);
        log.setReason(reason);
        log.setIsDelete(0);
        auditLogMapper.insert(log);
    }

    // ======================== 响应结构 ========================

    @Nested
    @DisplayName("响应结构验证")
    class ResponseStructureTests {

        @Test
        @DisplayName("接口返回成功且包含全部 5 个指标和 snapshotTime")
        void overviewStructureComplete() {
            AdminStatisticsOverviewDTO dto = fetchOverview();

            assertNotNull(dto.getPendingGoods(), "pendingGoods 不应为 null");
            assertNotNull(dto.getRiskUsers(), "riskUsers 不应为 null");
            assertNotNull(dto.getPendingFeedback(), "pendingFeedback 不应为 null");
            assertNotNull(dto.getPendingReports(), "pendingReports 不应为 null");
            assertNotNull(dto.getSolvedItems(), "solvedItems 不应为 null");
            assertNotNull(dto.getSnapshotTime(), "snapshotTime 不应为 null");
        }

        @Test
        @DisplayName("每个指标包含 count 和 trend 字段")
        void metricContainsCountAndTrend() {
            AdminStatisticsOverviewDTO dto = fetchOverview();

            for (String key : new String[]{"pendingGoods", "riskUsers", "pendingFeedback", "pendingReports", "solvedItems"}) {
                long count = getMetricCount(dto, key);
                String trend = getMetricTrend(dto, key);
                assertTrue(count >= 0, key + ".count 应 >= 0");
                assertNotNull(trend, key + ".trend 不应为 null");
                assertTrue(trend.endsWith("%"), key + ".trend 应以 % 结尾: " + trend);
            }
        }

        @Test
        @DisplayName("snapshotTime 格式为 yyyy-MM-dd HH:mm:ss")
        void snapshotTimeFormat() {
            AdminStatisticsOverviewDTO dto = fetchOverview();
            String snapshotTime = dto.getSnapshotTime();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime parsed = LocalDateTime.parse(snapshotTime, formatter);
            assertNotNull(parsed, "snapshotTime 应可解析为 LocalDateTime");
        }

        @Test
        @DisplayName("snapshotTime 在当前时间前后 5 秒内")
        void snapshotTimeIsRecent() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(5);
            AdminStatisticsOverviewDTO dto = fetchOverview();
            LocalDateTime after = LocalDateTime.now().plusSeconds(5);

            String snapshotTime = dto.getSnapshotTime();
            LocalDateTime parsed = LocalDateTime.parse(snapshotTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            assertTrue(!parsed.isBefore(before) && !parsed.isAfter(after),
                    "snapshotTime 应在当前时间附近: " + snapshotTime);
        }
    }

    // ======================== 待审商品统计 ========================

    @Nested
    @DisplayName("pendingGoods — 待审商品统计")
    class PendingGoodsTests {

        @Test
        @DisplayName("只统计 isAudited=0 且 is_delete=0 的商品")
        void countOnlyPendingGoods() {
            long before = getMetricCount(fetchOverview(), "pendingGoods");

            insertGoods(PUBLISHER, "待审商品A", 0, 0);
            insertGoods(PUBLISHER, "待审商品B", 0, 0);
            insertGoods(PUBLISHER, "已通过商品", 0, 1);
            insertGoods(PUBLISHER, "已驳回商品", 0, 2);

            long after = getMetricCount(fetchOverview(), "pendingGoods");
            assertEquals(before + 2, after, "只应统计 isAudited=0 的商品");
        }

        @Test
        @DisplayName("包含各 goodsStatus 下 isAudited=0 的商品")
        void includesAllGoodsStatuses() {
            long before = getMetricCount(fetchOverview(), "pendingGoods");

            insertGoods(PUBLISHER, "在售待审", 0, 0);
            insertGoods(PUBLISHER, "已售待审", 1, 0);
            insertGoods(PUBLISHER, "下架待审", 2, 0);
            insertGoods(PUBLISHER, "审核中", 3, 0);

            long after = getMetricCount(fetchOverview(), "pendingGoods");
            assertEquals(before + 4, after, "应包含所有 goodsStatus 下 isAudited=0 的商品");
        }

        @Test
        @DisplayName("不统计已删除商品")
        void excludesDeletedGoods() {
            insertGoods(PUBLISHER, "正常待审", 0, 0);
            long before = getMetricCount(fetchOverview(), "pendingGoods");

            TGoods deleted = new TGoods();
            deleted.setUserId(PUBLISHER);
            deleted.setGoodsName("已删除待审");
            deleted.setCategoryId(1);
            deleted.setGoodsType(1);
            deleted.setPrice(BigDecimal.TEN);
            deleted.setGoodsStatus(0);
            deleted.setIsAudited(0);
            deleted.setExchangePlace("测试地点");
            deleted.setIsDelete(1);
            goodsService.save(deleted);

            long after = getMetricCount(fetchOverview(), "pendingGoods");
            assertEquals(before, after, "不应统计 is_delete=1 的商品");
        }
    }

    // ======================== 风险用户统计 ========================

    @Nested
    @DisplayName("riskUsers — 风险用户统计")
    class RiskUsersTests {

        @Test
        @DisplayName("只统计 creditScore<70 且 status=2 的用户")
        void countOnlyRiskUsers() {
            long before = getMetricCount(fetchOverview(), "riskUsers");

            ensureUser(PREFIX + "_risk1", "风险用户1", 50, 2);
            ensureUser(PREFIX + "_risk2", "风险用户2", 60, 2);
            ensureUser(PREFIX + "_normal", "正常用户", 100, 0);
            ensureUser(PREFIX + "_low", "低分正常", 50, 0);

            long after = getMetricCount(fetchOverview(), "riskUsers");
            assertEquals(before + 2, after, "只应统计 creditScore<70 且 status=2 的用户");
        }

        @Test
        @DisplayName("不统计已删除的风险用户")
        void excludesDeletedRiskUsers() {
            long before = getMetricCount(fetchOverview(), "riskUsers");

            ensureUser(PREFIX + "_risk_del", "已删风险用户", 50, 2);
            TUser del = userService.getById(PREFIX + "_risk_del");
            del.setIsDelete(1);
            userService.updateById(del);

            long after = getMetricCount(fetchOverview(), "riskUsers");
            assertEquals(before, after, "不应统计 is_delete=1 的风险用户");
        }
    }

    // ======================== 待处理反馈统计 ========================

    @Nested
    @DisplayName("pendingFeedback — 待处理反馈统计")
    class PendingFeedbackTests {

        @Test
        @DisplayName("只统计 feedbackStatus=0 且 is_delete=0 的反馈")
        void countOnlyPendingFeedback() {
            long before = getMetricCount(fetchOverview(), "pendingFeedback");

            insertFeedback(FB_USER, "待处理反馈1", 0);
            insertFeedback(FB_USER, "待处理反馈2", 0);
            insertFeedback(FB_USER, "已处理反馈", 1);

            long after = getMetricCount(fetchOverview(), "pendingFeedback");
            assertEquals(before + 2, after, "只应统计 feedbackStatus=0 的反馈");
        }

        @Test
        @DisplayName("不统计已删除反馈")
        void excludesDeletedFeedback() {
            long before = getMetricCount(fetchOverview(), "pendingFeedback");

            insertFeedback(FB_USER, "正常待处理", 0);

            TFeedback deleted = new TFeedback();
            deleted.setUserId(FB_USER);
            deleted.setFeedbackContent("已删除反馈");
            deleted.setFeedbackStatus(0);
            deleted.setIsRead(0);
            deleted.setIsDelete(1);
            feedbackService.save(deleted);

            long after = getMetricCount(fetchOverview(), "pendingFeedback");
            assertEquals(before + 1, after, "不应统计 is_delete=1 的反馈");
        }
    }

    // ======================== 待处理举报统计 ========================

    @Nested
    @DisplayName("pendingReports — 待处理举报统计")
    class PendingReportsTests {

        @Test
        @DisplayName("只统计 status=0 的举报")
        void countOnlyPendingReports() {
            long before = getMetricCount(fetchOverview(), "pendingReports");

            ensureUser(PREFIX + "_target", "被举报目标", 100, 0);
            insertReport(REPORTER, PREFIX + "_target", "LOW_CREDIT", 0);
            insertReport(REPORTER, PREFIX + "_target", "GOODS_VIOLATION", 0);
            insertReport(REPORTER, PREFIX + "_target", "LANG_VIOLATION", 1);

            long after = getMetricCount(fetchOverview(), "pendingReports");
            assertEquals(before + 2, after, "只应统计 status=0 的举报");
        }
    }

    // ======================== 已解决事项统计 ========================

    @Nested
    @DisplayName("solvedItems — 已解决事项统计")
    class SolvedItemsTests {

        @Test
        @DisplayName("聚合已通过商品 + 已驳回商品 + 已处理反馈 + 审计日志用户数")
        void countSolvedItems() {
            long before = getMetricCount(fetchOverview(), "solvedItems");

            insertGoods(PUBLISHER, "已通过商品1", 0, 1);
            insertGoods(PUBLISHER, "已通过商品2", 0, 1);
            insertGoods(PUBLISHER, "已驳回商品", 0, 2);
            insertFeedback(FB_USER, "已处理反馈", 1);
            ensureUser(PREFIX + "_ban_user", "被封用户", 50, 1);
            insertAuditLog(PREFIX + "_ban_user", "ban", 2, "违规查封");

            long after = getMetricCount(fetchOverview(), "solvedItems");
            assertEquals(before + 5, after, "应聚合商品+反馈+用户维度的已解决事项");
        }

        @Test
        @DisplayName("不统计待审商品和待处理反馈")
        void excludesPendingItems() {
            long before = getMetricCount(fetchOverview(), "solvedItems");

            insertGoods(PUBLISHER, "待审商品", 0, 0);
            insertFeedback(FB_USER, "待处理反馈", 0);

            long after = getMetricCount(fetchOverview(), "solvedItems");
            assertEquals(before, after, "待审商品和待处理反馈不应计入已解决事项");
        }
    }

    // ======================== 空数据场景 ========================

    @Nested
    @DisplayName("空数据场景")
    class EmptyDataTests {

        @Test
        @DisplayName("不插入任何数据时，指标数量保持不变，trend 符合预期规则")
        void allZeroWhenEmpty() {
            AdminStatisticsOverviewDTO before = fetchOverview();

            // 不插入任何数据，再次获取
            AdminStatisticsOverviewDTO after = fetchOverview();

            assertEquals(getMetricCount(before, "pendingGoods"), getMetricCount(after, "pendingGoods"));
            assertEquals(getMetricCount(before, "riskUsers"), getMetricCount(after, "riskUsers"));
            assertEquals(getMetricCount(before, "pendingFeedback"), getMetricCount(after, "pendingFeedback"));
            assertEquals(getMetricCount(before, "pendingReports"), getMetricCount(after, "pendingReports"));
            assertEquals(getMetricCount(before, "solvedItems"), getMetricCount(after, "solvedItems"));

            // trend 规则：yesterday=0 时，today=0 → "0%"，today>0 → "+100%"
            for (String key : new String[]{"pendingGoods", "riskUsers", "pendingFeedback", "pendingReports", "solvedItems"}) {
                long count = getMetricCount(after, key);
                String expectedTrend = count > 0 ? "+100%" : "0%";
                assertEquals(expectedTrend, getMetricTrend(after, key),
                        key + " 无历史数据时 trend 应为 " + expectedTrend);
            }
        }
    }
}