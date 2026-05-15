package com.bit.scmu_taotao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackDetailDTO;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackListItemDTO;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackMarkUnreadRequest;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackPageRequest;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackResolveRequest;
import com.bit.scmu_taotao.entity.TFeedback;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.TFeedbackService;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("管理员反馈管理集成测试")
class AdminFeedbackControllerTest {

    @Autowired
    private AdminFeedbackController controller;

    @Autowired
    private TFeedbackService feedbackService;

    @Autowired
    private TUserService tUserService;

    private static final String TEST_PREFIX = "ut_admin_fb";
    private static final String TEST_USER_ID = TEST_PREFIX + "_user";
    private static final String TEST_USER_NAME = "反馈测试用户";

    private Long pendingId;
    private Long readPendingId;
    private Long processedId;

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureTestUser();
        TFeedback pending = newFeedback(TEST_USER_ID, "建议增加夜间模式", 0, 0);
        feedbackService.save(pending);
        pendingId = pending.getFeedbackId();
        TFeedback readPending = newFeedback(TEST_USER_ID, "希望优化搜索速度", 0, 1);
        feedbackService.save(readPending);
        readPendingId = readPending.getFeedbackId();
        TFeedback processed = newFeedback(TEST_USER_ID, "登录页面加载慢", 1, 1);
        processed.setReplyContent("已修复，请刷新后重试");
        processed.setReplyTime(LocalDateTime.now().minusDays(1));
        feedbackService.save(processed);
        processedId = processed.getFeedbackId();
    }

    @AfterEach
    void tearDown() { cleanTestData(); }

    private void cleanTestData() {
        feedbackService.remove(new LambdaQueryWrapper<TFeedback>().eq(TFeedback::getUserId, TEST_USER_ID));
        tUserService.remove(new LambdaQueryWrapper<TUser>().eq(TUser::getUserId, TEST_USER_ID));
    }

    private void ensureTestUser() {
        TUser user = tUserService.getById(TEST_USER_ID);
        if (user == null) {
            user = new TUser();
            user.setUserId(TEST_USER_ID);
            user.setUserName(TEST_USER_NAME);
            user.setAvatar("/avatar/test.png");
            user.setCreditScore(100);
            user.setCreditStar(java.math.BigDecimal.valueOf(4.0));
            user.setStatus(0);
            user.setIsDelete(0);
            tUserService.save(user);
        }
    }

    private TFeedback newFeedback(String userId, String content, int status, int isRead) {
        TFeedback f = new TFeedback();
        f.setUserId(userId);
        f.setFeedbackContent(content);
        f.setFeedbackStatus(status);
        f.setIsRead(isRead);
        f.setIsDelete(0);
        return f;
    }

    private BindingResult validBindingResult(Object target) {
        return new BeanPropertyBindingResult(target, "request");
    }

    @Nested
    @DisplayName("GET /admin/feedback/list")
    class ListTests {

        @Test
        @DisplayName("默认分页返回全部未删除反馈")
        void listAll() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setPage(1);
            req.setSize(10);
            Result result = controller.list(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertNotNull(data);
            @SuppressWarnings("unchecked")
            List<AdminFeedbackListItemDTO> list = (List<AdminFeedbackListItemDTO>) data.get("list");
            assertFalse(list.isEmpty(), "反馈列表不应为空");
            assertTrue(list.stream().anyMatch(item -> TEST_USER_ID.equals(item.getUserId())),
                    "应包含测试用户的反馈");
        }

        @Test
        @DisplayName("status=pending 仅返回待处理项")
        void listByPendingStatus() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setStatus("pending");
            Result result = controller.list(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<AdminFeedbackListItemDTO> list = (List<AdminFeedbackListItemDTO>) data.get("list");
            for (AdminFeedbackListItemDTO item : list) {
                assertEquals("pending", item.getStatus());
            }
        }

        @Test
        @DisplayName("status=processed 仅返回已处理项")
        void listByProcessedStatus() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setStatus("processed");
            Result result = controller.list(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<AdminFeedbackListItemDTO> list = (List<AdminFeedbackListItemDTO>) data.get("list");
            for (AdminFeedbackListItemDTO item : list) {
                assertEquals("processed", item.getStatus());
            }
        }

        @Test
        @DisplayName("keyword 按用户名模糊搜索")
        void listByUserNameKeyword() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setKeyword(TEST_USER_NAME);
            Result result = controller.list(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<AdminFeedbackListItemDTO> list = (List<AdminFeedbackListItemDTO>) data.get("list");
            assertFalse(list.isEmpty());
            for (AdminFeedbackListItemDTO item : list) {
                assertEquals(TEST_USER_NAME, item.getUserName());
            }
        }

        @Test
        @DisplayName("keyword 按反馈内容模糊搜索")
        void listByContentKeyword() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setKeyword("夜间模式");
            Result result = controller.list(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            @SuppressWarnings("unchecked")
            List<AdminFeedbackListItemDTO> list = (List<AdminFeedbackListItemDTO>) data.get("list");
            assertFalse(list.isEmpty());
            String content = list.get(0).getContent();
            assertTrue(content.contains("夜间模式"));
        }

        @Test
        @DisplayName("不存在的关键词返回空列表")
        void listByNonExistentKeyword() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setKeyword("不存在的内容XYZ123");
            Result result = controller.list(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertEquals(0L, data.get("total"));
        }
    }

    @Nested
    @DisplayName("GET /admin/feedback/{feedbackId}")
    class DetailTests {

        @Test
        @DisplayName("查询详情自动标记为已读")
        void detailAutoMarkRead() {
            Result result = controller.detail(pendingId);
            assertEquals(200, result.getCode());
            AdminFeedbackDetailDTO dto = (AdminFeedbackDetailDTO) result.getData();
            assertEquals("read", dto.getIs_read());
            TFeedback updated = feedbackService.getById(pendingId);
            assertEquals(1, updated.getIsRead());
        }

        @Test
        @DisplayName("查询不存在的反馈返回404")
        void detailNotFound() {
            Result result = controller.detail(99999999L);
            assertEquals(404, result.getCode());
        }

        @Test
        @DisplayName("详情包含用户名和头像")
        void detailContainsUserInfo() {
            Result result = controller.detail(readPendingId);
            assertEquals(200, result.getCode());
            AdminFeedbackDetailDTO dto = (AdminFeedbackDetailDTO) result.getData();
            assertEquals(TEST_USER_NAME, dto.getUserName());
            assertEquals(TEST_USER_ID, dto.getUserId());
            assertNotNull(dto.getAvatar());
        }
    }

    @Nested
    @DisplayName("PUT /admin/feedback/mark-unread")
    class MarkUnreadTests {

        @Test
        @DisplayName("单条标记未读")
        void markSingleUnread() {
            AdminFeedbackMarkUnreadRequest req = new AdminFeedbackMarkUnreadRequest();
            req.setFeedbackIds(List.of(readPendingId));
            Result result = controller.markUnread(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            TFeedback updated = feedbackService.getById(readPendingId);
            assertEquals(0, updated.getIsRead());
        }

        @Test
        @DisplayName("批量标记未读")
        void markMultipleUnread() {
            AdminFeedbackMarkUnreadRequest req = new AdminFeedbackMarkUnreadRequest();
            req.setFeedbackIds(List.of(pendingId, readPendingId, processedId));
            Result result = controller.markUnread(req, validBindingResult(req));
            assertEquals(200, result.getCode());
            for (Long id : List.of(pendingId, readPendingId, processedId)) {
                TFeedback f = feedbackService.getById(id);
                assertEquals(0, f.getIsRead());
            }
        }

        @Test
        @DisplayName("空ID列表返回400")
        void markEmptyList() {
            AdminFeedbackMarkUnreadRequest req = new AdminFeedbackMarkUnreadRequest();
            req.setFeedbackIds(List.of());
            BeanPropertyBindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("feedbackIds", "NotEmpty", "feedbackIds不能为空");
            Result result = controller.markUnread(req, br);
            assertEquals(400, result.getCode());
        }
    }

    @Nested
    @DisplayName("POST /admin/feedback/{feedbackId}/resolve")
    class ResolveTests {

        @Test
        @DisplayName("成功处理待处理反馈")
        void resolvePendingFeedback() {
            AdminFeedbackResolveRequest req = new AdminFeedbackResolveRequest();
            req.setReplyContent("感谢反馈，已优化夜间模式");
            Result result = controller.resolve(pendingId, req, validBindingResult(req));
            assertEquals(200, result.getCode());
            TFeedback updated = feedbackService.getById(pendingId);
            assertEquals(1, updated.getFeedbackStatus());
            assertEquals("感谢反馈，已优化夜间模式", updated.getReplyContent());
            assertNotNull(updated.getReplyTime());
        }

        @Test
        @DisplayName("已处理反馈再次提交返回409")
        void resolveAlreadyProcessed() {
            AdminFeedbackResolveRequest req = new AdminFeedbackResolveRequest();
            req.setReplyContent("重复回复");
            Result result = controller.resolve(processedId, req, validBindingResult(req));
            assertEquals(409, result.getCode());
        }

        @Test
        @DisplayName("不存在的反馈返回404")
        void resolveNotFound() {
            AdminFeedbackResolveRequest req = new AdminFeedbackResolveRequest();
            req.setReplyContent("回复内容");
            Result result = controller.resolve(99999999L, req, validBindingResult(req));
            assertEquals(404, result.getCode());
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @Test
        @DisplayName("page 小于1 返回400")
        void invalidPage() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setPage(0);
            BeanPropertyBindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("page", "Min", "page必须大于等于1");
            Result result = controller.list(req, br);
            assertEquals(400, result.getCode());
        }

        @Test
        @DisplayName("size 超过50 返回400")
        void invalidSize() {
            AdminFeedbackPageRequest req = new AdminFeedbackPageRequest();
            req.setSize(100);
            BeanPropertyBindingResult br = new BeanPropertyBindingResult(req, "request");
            br.rejectValue("size", "Max", "size不能超过50");
            Result result = controller.list(req, br);
            assertEquals(400, result.getCode());
        }
    }
}
