package com.bit.scmu_taotao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.UserReportRequest;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.entity.TUserReport;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户举报服务层集成测试
 * 测试举报功能的所有业务逻辑
 */
@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
@DisplayName("用户举报服务集成测试")
class TUserReportServiceTest {

    @Autowired
    private TUserReportService tUserReportService;

    @Autowired
    private TUserService tUserService;

    private static final String TEST_PREFIX = "ut_report_service";
    private static final String REPORTER_ID = TEST_PREFIX + "_reporter";
    private static final String TARGET_ID = TEST_PREFIX + "_target";

    @BeforeEach
    void setUp() {
        cleanTestData();
        ensureUser(REPORTER_ID, "举报人");
        ensureUser(TARGET_ID, "被举报人");
        UserContext.setUserId(REPORTER_ID);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
        UserContext.remove();
    }

    private void cleanTestData() {
        tUserReportService.remove(new LambdaQueryWrapper<TUserReport>()
                .likeRight(TUserReport::getReporterId, TEST_PREFIX + "_")
                .or().likeRight(TUserReport::getTargetId, TEST_PREFIX + "_"));
        tUserService.remove(new LambdaQueryWrapper<TUser>()
                .likeRight(TUser::getUserId, TEST_PREFIX + "_"));
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

    // ======================== 正常场景测试 ========================

    @Test
    @DisplayName("reportUser: 正常举报含图片")
    void testReportUserWithImageSuccess() {
        UserReportRequest request = new UserReportRequest();
        request.setCategory("GOODS_VIOLATION");
        request.setContent("此用户发布违法商品");
        request.setImgUrls(List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg"));

        Result result = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(200, result.getCode());
        assertTrue(result.getMsg().contains("成功"));

        TUserReport saved = tUserReportService.getOne(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, REPORTER_ID)
                .eq(TUserReport::getTargetId, TARGET_ID));
        assertNotNull(saved);
        assertEquals("GOODS_VIOLATION", saved.getTag());
        assertEquals("此用户发布违法商品", saved.getContent());
        assertEquals(0, saved.getStatus());
        assertTrue(saved.getImgUrls().contains("https://example.com/img1.jpg"));
    }

    @Test
    @DisplayName("reportUser: 正常举报无图片")
    void testReportUserWithoutImageSuccess() {
        UserReportRequest request = new UserReportRequest();
        request.setCategory("LOW_CREDIT");
        request.setContent("用户诚信度低");
        request.setImgUrls(null);

        Result result = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(200, result.getCode());

        TUserReport saved = tUserReportService.getOne(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, REPORTER_ID)
                .eq(TUserReport::getTargetId, TARGET_ID));
        assertNotNull(saved);
        assertNull(saved.getImgUrls());
    }

    @Test
    @DisplayName("reportUser: 正常举报空图片列表")
    void testReportUserWithEmptyImageListSuccess() {
        UserReportRequest request = new UserReportRequest();
        request.setCategory("OTHER");
        request.setContent("其他举报");
        request.setImgUrls(List.of());

        Result result = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(200, result.getCode());

        TUserReport saved = tUserReportService.getOne(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, REPORTER_ID)
                .eq(TUserReport::getTargetId, TARGET_ID));
        assertNotNull(saved);
        assertNull(saved.getImgUrls());
    }

    // ======================== 业务规则校验失败 ========================

    @Test
    @DisplayName("reportUser: 举报自己")
    void testReportUserSelfFail() {
        UserReportRequest request = new UserReportRequest();
        request.setCategory("LOW_CREDIT");
        request.setContent("举报自己");
        request.setImgUrls(null);

        Result result = tUserReportService.reportUser(REPORTER_ID, request);
        assertEquals(400, result.getCode());
        assertTrue(result.getMsg().contains("不能举报自己"));
    }

    @Test
    @DisplayName("reportUser: 24小时内重复举报同一用户")
    void testReportUserDuplicate24hFail() {
        UserReportRequest request = new UserReportRequest();
        request.setCategory("LANG_VIOLATION");
        request.setContent("用户言论不当");
        request.setImgUrls(null);

        // 第一次举报
        Result result1 = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(200, result1.getCode());

        // 24小时内重复举报
        Result result2 = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(400, result2.getCode());
        assertTrue(result2.getMsg().contains("24小时内"));
    }

    @Test
    @DisplayName("reportUser: 被举报用户不存在")
    void testReportUserTargetNotExistsFail() {
        String nonExistentUserId = TEST_PREFIX + "_non_existent";

        UserReportRequest request = new UserReportRequest();
        request.setCategory("LOW_CREDIT");
        request.setContent("用户不存在时举报");
        request.setImgUrls(null);

        Result result = tUserReportService.reportUser(nonExistentUserId, request);
        assertEquals(404, result.getCode());
        assertTrue(result.getMsg().contains("用户不存在"));
    }

    @Test
    @DisplayName("reportUser: 未登录")
    void testReportUserNotLoggedInFail() {
        UserContext.remove();

        UserReportRequest request = new UserReportRequest();
        request.setCategory("LOW_CREDIT");
        request.setContent("未登录举报");
        request.setImgUrls(null);

        Result result = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(401, result.getCode());
        assertTrue(result.getMsg().contains("未登录"));
    }

    // ======================== 所有category有效值 ========================

    @Test
    @DisplayName("reportUser: 所有category有效值都可接受")
    void testReportUserAllValidCategoriesSuccess() {
        String[] categories = {"LOW_CREDIT", "GOODS_VIOLATION", "LANG_VIOLATION", "OTHER"};

        for (int i = 0; i < categories.length; i++) {
            String targetId = TARGET_ID + "_" + i;
            ensureUser(targetId, "被举报人_" + i);

            UserReportRequest request = new UserReportRequest();
            request.setCategory(categories[i]);
            request.setContent("举报内容_" + categories[i]);
            request.setImgUrls(null);

            Result result = tUserReportService.reportUser(targetId, request);
            assertEquals(200, result.getCode());

            TUserReport saved = tUserReportService.getOne(new LambdaQueryWrapper<TUserReport>()
                    .eq(TUserReport::getReporterId, REPORTER_ID)
                    .eq(TUserReport::getTargetId, targetId));
            assertNotNull(saved);
            assertEquals(categories[i], saved.getTag());
        }
    }

    // ======================== 边界值测试 ========================

    @Test
    @DisplayName("reportUser: content边界值（500字）")
    void testReportUserContentBoundary() {
        String content = "a".repeat(500);

        UserReportRequest request = new UserReportRequest();
        request.setCategory("GOODS_VIOLATION");
        request.setContent(content);
        request.setImgUrls(null);

        Result result = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(200, result.getCode());

        TUserReport saved = tUserReportService.getOne(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, REPORTER_ID)
                .eq(TUserReport::getTargetId, TARGET_ID));
        assertNotNull(saved);
        assertEquals(content, saved.getContent());
    }

    @Test
    @DisplayName("reportUser: imgUrls边界值（3张）")
    void testReportUserImageBoundary() {
        UserReportRequest request = new UserReportRequest();
        request.setCategory("GOODS_VIOLATION");
        request.setContent("举报内容");
        request.setImgUrls(List.of(
                "https://example.com/img1.jpg",
                "https://example.com/img2.jpg",
                "https://example.com/img3.jpg"
        ));

        Result result = tUserReportService.reportUser(TARGET_ID, request);
        assertEquals(200, result.getCode());

        TUserReport saved = tUserReportService.getOne(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, REPORTER_ID)
                .eq(TUserReport::getTargetId, TARGET_ID));
        assertNotNull(saved);
        assertTrue(saved.getImgUrls().contains("https://example.com/img3.jpg"));
    }
}

