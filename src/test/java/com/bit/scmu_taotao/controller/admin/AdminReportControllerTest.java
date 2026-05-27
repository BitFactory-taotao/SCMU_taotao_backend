package com.bit.scmu_taotao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.AdminReportDetailDTO;
import com.bit.scmu_taotao.dto.admin.AdminReportListItemDTO;
import com.bit.scmu_taotao.dto.admin.AdminReportPageRequest;
import com.bit.scmu_taotao.dto.admin.AdminReportVerifyRequest;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.TCreditLog;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.entity.TUserReport;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.service.TCreditLogService;
import com.bit.scmu_taotao.service.TUserReportService;
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
import com.bit.scmu_taotao.service.StompPushService;
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
@DisplayName("管理员举报审核集成测试")
class AdminReportControllerTest {

    @Autowired
    private AdminReportController controller;

    @Autowired
    private TUserReportService reportService;

    @Autowired
    private TUserService userService;

    @Autowired
    private TCreditLogService creditLogService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @MockBean
    private StompPushService stompPushService;

    private static final String PREFIX = "ut_admin_report";
    private static final String REPORTER = PREFIX + "_reporter";
    private static final String TARGET = PREFIX + "_target";

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(REPORTER, "举报人A");
        ensureUser(TARGET, "被举报人B");
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
        reportService.remove(new LambdaQueryWrapper<TUserReport>()
                .likeRight(TUserReport::getReporterId, PREFIX + "_"));
        userService.remove(new LambdaQueryWrapper<TUser>()
                .likeRight(TUser::getUserId, PREFIX + "_"));
    }

    private void ensureUser(String userId, String userName) {
        if (userService.getById(userId) != null) return;
        TUser u = new TUser();
        u.setUserId(userId);
        u.setUserName(userName);
        u.setAvatar("https://example.com/avatar.png");
        u.setCreditScore(100);
        u.setCreditStar(BigDecimal.valueOf(5.0));
        u.setStatus(0);
        u.setIsDelete(0);
        userService.save(u);
    }

    /** 插入一条 status=0 的举报记录，返回主键 */
    private Long insertPendingReport(String tag, String content) {
        TUserReport r = new TUserReport();
        r.setReporterId(REPORTER);
        r.setTargetId(TARGET);
        r.setTag(tag);
        r.setContent(content);
        r.setImgUrls("https://img.com/a.jpg,https://img.com/b.jpg");
        r.setStatus(0);
        reportService.save(r);
        return r.getId();
    }

    /** 插入一条 status=1 的举报记录（已处理），返回主键 */
    private Long insertProcessedReport() {
        TUserReport r = new TUserReport();
        r.setReporterId(REPORTER);
        r.setTargetId(TARGET);
        r.setTag("OTHER");
        r.setContent("已处理的举报");
        r.setStatus(1);
        reportService.save(r);
        return r.getId();
    }

    private BindingResult bind(Object target) {
        return new BeanPropertyBindingResult(target, "request");
    }

    // ======================== 列表 ========================

    @Nested
    @DisplayName("GET /admin/reports — 举报列表")
    class ListTests {

        @Test
        @DisplayName("默认分页只返回待审核（status=0）记录")
        void listOnlyPending() {
            Long pendingId = insertPendingReport("GOODS_VIOLATION", "商品违规");
            insertProcessedReport();

            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<AdminReportListItemDTO> list = (List<AdminReportListItemDTO>) result.getData();
            assertFalse(list.isEmpty());
            // 每条记录的 status 都应为 0
            assertTrue(list.stream().allMatch(item -> item.getStatus() == 0),
                    "列表应只包含 status=0 的记录");
            // 不应包含已处理的记录
            boolean hasProcessed = list.stream()
                    .anyMatch(item -> item.getReportId().equals(insertProcessedReport()));
            assertFalse(hasProcessed, "不应包含已处理记录");
        }

        @Test
        @DisplayName("keyword 按被举报人姓名模糊搜索")
        void listKeywordByUserName() {
            insertPendingReport("LANG_VIOLATION", "言论违规");

            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setKeyword("被举报人");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<AdminReportListItemDTO> list = (List<AdminReportListItemDTO>) result.getData();
            assertFalse(list.isEmpty(), "按姓名搜索应有结果");
            assertTrue(list.stream().allMatch(item ->
                    TARGET.equals(item.getTargetUser().getId())));
        }

        @Test
        @DisplayName("keyword 按被举报人学号搜索")
        void listKeywordByUserId() {
            insertPendingReport("LOW_CREDIT", "信用不良");

            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setKeyword(TARGET);
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<AdminReportListItemDTO> list = (List<AdminReportListItemDTO>) result.getData();
            assertFalse(list.isEmpty());
        }

        @Test
        @DisplayName("keyword 无匹配时返回空列表")
        void listKeywordNoMatch() {
            insertPendingReport("OTHER", "其他");

            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setKeyword("不存在的用户XYZ");
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<AdminReportListItemDTO> list = (List<AdminReportListItemDTO>) result.getData();
            assertTrue(list.isEmpty(), "无匹配时列表应为空");
        }

        @Test
        @DisplayName("列表项包含 targetUser 信息和 tagDesc")
        void listItemContainsTargetUserAndTagDesc() {
            insertPendingReport("GOODS_VIOLATION", "商品违规举报");

            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, bind(req));

            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            List<AdminReportListItemDTO> list = (List<AdminReportListItemDTO>) result.getData();
            AdminReportListItemDTO item = list.get(0);

            assertNotNull(item.getReportId());
            assertNotNull(item.getTargetUser());
            assertEquals(TARGET, item.getTargetUser().getId());
            assertEquals("被举报人B", item.getTargetUser().getName());
            assertNotNull(item.getTargetUser().getAvatar());
            assertEquals("GOODS_VIOLATION", item.getTag());
            assertEquals("商品违规", item.getTagDesc());
            assertNotNull(item.getCreateTime());
        }
    }

    // ======================== 详情 ========================

    @Nested
    @DisplayName("GET /admin/reports/{reportId} — 举报详情")
    class DetailTests {

        @Test
        @DisplayName("成功获取详情，包含图片列表")
        void detailSuccess() {
            Long id = insertPendingReport("GOODS_VIOLATION", "商品描述与实际严重不符");

            Result result = controller.detail(id);
            assertEquals(200, result.getCode());

            AdminReportDetailDTO dto = (AdminReportDetailDTO) result.getData();
            assertEquals(id, dto.getReportId());
            assertEquals(TARGET, dto.getTargetUser().getId());
            assertEquals("被举报人B", dto.getTargetUser().getName());
            assertEquals("GOODS_VIOLATION", dto.getTag());
            assertEquals("商品违规", dto.getTagDesc());
            assertEquals("商品描述与实际严重不符", dto.getContent());
            assertNotNull(dto.getImgUrls());
            assertEquals(2, dto.getImgUrls().size());
            assertTrue(dto.getImgUrls().get(0).contains("a.jpg"));
            assertNotNull(dto.getCreateTime());
        }

        @Test
        @DisplayName("不存在的 reportId 返回 404")
        void detailNotFound() {
            Result result = controller.detail(99999999L);
            assertEquals(404, result.getCode());
        }
    }

    // ======================== 处理举报（PASS） ========================

    @Nested
    @DisplayName("PUT /admin/reports/{reportId}/verify — PASS（属实）")
    class VerifyPassTests {

        @Test
        @DisplayName("PASS 成功：扣分 + 写 creditLog + 更新状态")
        void verifyPassSuccess() {
            Long reportId = insertPendingReport("GOODS_VIOLATION", "商品违规");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(10);

            Result result = controller.verify(reportId, req, bind(req));
            assertEquals(200, result.getCode());

            // 验证返回数据
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertEquals(reportId, ((Number) data.get("reportId")).longValue());
            assertEquals(TARGET, data.get("targetUserId"));
            assertEquals(90, ((Number) data.get("newCreditScore")).intValue());
            assertEquals("PROCESSED", data.get("status"));

            // 验证举报状态已更新
            TUserReport updated = reportService.getById(reportId);
            assertEquals(1, updated.getStatus());

            // 验证被举报人信用分已扣除
            TUser targetUser = userService.getById(TARGET);
            assertEquals(90, targetUser.getCreditScore());

            // 验证 creditLog 已写入
            List<TCreditLog> logs = creditLogService.list(new LambdaQueryWrapper<TCreditLog>()
                    .eq(TCreditLog::getUserId, TARGET));
            assertFalse(logs.isEmpty(), "应有信用变动记录");
            TCreditLog log = logs.get(0);
            assertEquals(-10, log.getScoreChange());
            assertEquals("GOODS_VIOLATION", log.getChangeType());
            assertTrue(log.getReason().contains("扣除10分"));
        }

        @Test
        @DisplayName("PASS 重复处理返回 400")
        void verifyPassAlreadyProcessed() {
            Long reportId = insertPendingReport("LANG_VIOLATION", "言论违规");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(5);

            // 第一次处理
            controller.verify(reportId, req, bind(req));

            // 第二次重复处理
            Result result = controller.verify(reportId, req, bind(req));
            assertEquals(400, result.getCode());
            assertTrue(result.getMsg().contains("已处理"));
        }

        @Test
        @DisplayName("PASS 不存在的 reportId 返回 404")
        void verifyPassNotFound() {
            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(5);

            Result result = controller.verify(99999999L, req, bind(req));
            assertEquals(404, result.getCode());
        }

        @Test
        @DisplayName("PASS 时 deductScore 为 null 返回 400")
        void verifyPassNullDeductScore() {
            Long reportId = insertPendingReport("OTHER", "其他");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(null);

            Result result = controller.verify(reportId, req, bind(req));
            assertEquals(400, result.getCode());
            assertTrue(result.getMsg().contains("扣分"));
        }

        @Test
        @DisplayName("PASS 时 deductScore 为 0 返回 400")
        void verifyPassZeroDeductScore() {
            Long reportId = insertPendingReport("OTHER", "其他");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(0);

            Result result = controller.verify(reportId, req, bind(req));
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("PASS 扣分后信用分不低于 0")
        void verifyPassCreditScoreFloorAtZero() {
            // 将被举报人信用分设为 3
            TUser target = userService.getById(TARGET);
            target.setCreditScore(3);
            userService.updateById(target);

            Long reportId = insertPendingReport("LOW_CREDIT", "信用不良");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(10);

            Result result = controller.verify(reportId, req, bind(req));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertEquals(0, ((Number) data.get("newCreditScore")).intValue());

            TUser updated = userService.getById(TARGET);
            assertEquals(0, updated.getCreditScore());
        }
    }

    // ======================== 处理举报（REJECT） ========================

    @Nested
    @DisplayName("PUT /admin/reports/{reportId}/verify — REJECT（驳回）")
    class VerifyRejectTests {

        @Test
        @DisplayName("REJECT 成功：更新状态，信用分不变")
        void verifyRejectSuccess() {
            Long reportId = insertPendingReport("LANG_VIOLATION", "言论违规");
            int originalScore = userService.getById(TARGET).getCreditScore();

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("REJECT");

            Result result = controller.verify(reportId, req, bind(req));
            assertEquals(200, result.getCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertEquals("PROCESSED", data.get("status"));
            assertEquals(originalScore, ((Number) data.get("newCreditScore")).intValue());

            // 验证举报状态已更新
            TUserReport updated = reportService.getById(reportId);
            assertEquals(1, updated.getStatus());

            // 验证信用分未变
            TUser targetUser = userService.getById(TARGET);
            assertEquals(originalScore, targetUser.getCreditScore());

            // 验证无 creditLog 写入
            List<TCreditLog> logs = creditLogService.list(new LambdaQueryWrapper<TCreditLog>()
                    .eq(TCreditLog::getUserId, TARGET));
            assertTrue(logs.isEmpty(), "REJECT 不应写入信用变动记录");
        }

        @Test
        @DisplayName("REJECT 不存在的 reportId 返回 404")
        void verifyRejectNotFound() {
            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("REJECT");

            Result result = controller.verify(99999999L, req, bind(req));
            assertEquals(404, result.getCode());
        }
    }

    // ======================== 参数校验 ========================

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @Test
        @DisplayName("page 小于 1 返回 400")
        void invalidPage() {
            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setPage(0);
            req.setSize(10);
            BeanPropertyBindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("page", "Min", "page必须大于等于1");
            Result result = controller.list(req, br);
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("size 超过 50 返回 400")
        void invalidSize() {
            AdminReportPageRequest req = new AdminReportPageRequest();
            req.setPage(1);
            req.setSize(100);
            BeanPropertyBindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("size", "Max", "size不能超过50");
            Result result = controller.list(req, br);
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("action 非法值返回 400")
        void invalidAction() {
            Long reportId = insertPendingReport("OTHER", "其他");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("INVALID");

            BeanPropertyBindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("action", "Pattern", "action必须是PASS或REJECT");
            Result result = controller.verify(reportId, req, br);
            assertEquals(400, result.getCode());
        }
    }

    // ======================== STOMP 推送通知 ========================

    @Nested
    @DisplayName("STOMP 推送通知验证")
    class PushNotificationTests {

        private void assertSystemPayload(Map<String, Object> payload, String expectedContent) {
            assertEquals("system", payload.get("type"));
            assertEquals(expectedContent, payload.get("content"));
            assertNotNull(payload.get("createTime"));
        }

        private ChatMessage getSystemMessage(String receiveId) {
            return chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSendId, "system")
                    .eq(ChatMessage::getReceiveId, receiveId)
                    .orderByDesc(ChatMessage::getMsgId)
                    .last("limit 1"));
        }

        @Test
        @DisplayName("PASS：举报人收到系统消息推送")
        void passNotifyReporter() {
            Long reportId = insertPendingReport("GOODS_VIOLATION", "商品违规");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(5);
            controller.verify(reportId, req, bind(req));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(stompPushService).pushToUserQueue(
                    Mockito.eq(REPORTER),
                    Mockito.eq("/queue/messages"),
                    captor.capture());

            Map<String, Object> payload = captor.getValue();
            assertSystemPayload(payload, "您提交的举报已审核处理完毕，结果：属实，已对被举报人扣除5分。");

            ChatMessage message = getSystemMessage(REPORTER);
            assertNotNull(message);
            assertEquals("system", message.getSendId());
            assertEquals(REPORTER, message.getReceiveId());
            assertEquals(Integer.valueOf(0), message.getMsgType());
            assertEquals("您提交的举报已审核处理完毕，结果：属实，已对被举报人扣除5分。", message.getMsgContent());
            assertNotNull(message.getCreateTime());
        }

        @Test
        @DisplayName("PASS：被举报人收到系统消息推送")
        void passNotifyTarget() {
            Long reportId = insertPendingReport("GOODS_VIOLATION", "商品违规");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(8);
            controller.verify(reportId, req, bind(req));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(stompPushService).pushToUserQueue(
                    Mockito.eq(TARGET),
                    Mockito.eq("/queue/messages"),
                    captor.capture());

            Map<String, Object> payload = captor.getValue();
            assertSystemPayload(payload, "您因【商品违规】被举报属实，信誉分扣除8分，当前信誉分92。");

            ChatMessage message = getSystemMessage(TARGET);
            assertNotNull(message);
            assertEquals("system", message.getSendId());
            assertEquals(TARGET, message.getReceiveId());
            assertEquals(Integer.valueOf(0), message.getMsgType());
            assertEquals("您因【商品违规】被举报属实，信誉分扣除8分，当前信誉分92。", message.getMsgContent());
            assertNotNull(message.getCreateTime());
        }

        @Test
        @DisplayName("PASS：共触发 2 次推送（举报人 + 被举报人）")
        void passTriggerTwoPushes() {
            Long reportId = insertPendingReport("LANG_VIOLATION", "言论违规");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("PASS");
            req.setDeductScore(3);
            controller.verify(reportId, req, bind(req));

            // 验证总共调用了 2 次 pushToUserQueue
            Mockito.verify(stompPushService, Mockito.times(2))
                    .pushToUserQueue(Mockito.anyString(), Mockito.eq("/queue/messages"), Mockito.any());

            assertEquals(2L, chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSendId, "system")
                    .in(ChatMessage::getReceiveId, REPORTER, TARGET)));
        }

        @Test
        @DisplayName("REJECT：举报人收到系统消息推送")
        void rejectNotifyReporter() {
            Long reportId = insertPendingReport("OTHER", "其他");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("REJECT");
            controller.verify(reportId, req, bind(req));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(stompPushService).pushToUserQueue(
                    Mockito.eq(REPORTER),
                    Mockito.eq("/queue/messages"),
                    captor.capture());

            Map<String, Object> payload = captor.getValue();
            assertSystemPayload(payload, "您提交的举报已审核处理完毕，结果：不属实。");

            ChatMessage message = getSystemMessage(REPORTER);
            assertNotNull(message);
            assertEquals("system", message.getSendId());
            assertEquals(REPORTER, message.getReceiveId());
            assertEquals(Integer.valueOf(0), message.getMsgType());
            assertEquals("您提交的举报已审核处理完毕，结果：不属实。", message.getMsgContent());
            assertNotNull(message.getCreateTime());
        }

        @Test
        @DisplayName("REJECT：仅触发 1 次推送（举报人）")
        void rejectNoNotifyTarget() {
            Long reportId = insertPendingReport("OTHER", "其他");

            AdminReportVerifyRequest req = new AdminReportVerifyRequest();
            req.setAction("REJECT");
            controller.verify(reportId, req, bind(req));

            // 只调用 1 次（仅举报人），不应对 TARGET 调用
            Mockito.verify(stompPushService, Mockito.times(1))
                    .pushToUserQueue(Mockito.anyString(), Mockito.eq("/queue/messages"), Mockito.any());
            Mockito.verify(stompPushService, Mockito.never())
                    .pushToUserQueue(Mockito.eq(TARGET), Mockito.anyString(), Mockito.any());

            assertEquals(1L, chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSendId, "system")
                    .eq(ChatMessage::getReceiveId, REPORTER)));
        }
    }
}

