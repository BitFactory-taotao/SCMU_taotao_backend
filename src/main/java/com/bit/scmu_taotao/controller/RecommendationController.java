package com.bit.scmu_taotao.controller;

import com.bit.scmu_taotao.dto.recommend.BrowseRecordResponseDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendListResponseDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendationStatisticsDTO;
import com.bit.scmu_taotao.dto.recommend.UserCategoryPreferenceDTO;
import com.bit.scmu_taotao.service.RecommendationService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐系统控制器
 *
 * 提供推荐商品查询、浏览记录埋点、缓存管理等接口
 *
 * @author 推荐系统
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/goods")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取用户偏好分类（仅Debug用）
     *
     * 用于展示用户的浏览偏好分析，返回用户Top2浏览分类及占比
     *
     * @return 用户偏好分类列表
     *
     * 需要登录，请求示例:
     * GET /api/v1/campus-taotao/goods/preference/categories
     */
    @GetMapping("/preference/categories")
    public Result getUserPreferenceCategories() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        try {
            log.debug("获取用户偏好分类, userId={}", userId);

            List<UserCategoryPreferenceDTO> preferences =
                    recommendationService.getUserPreferenceCategories(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("top2Categories", preferences);

            log.debug("获取偏好分类成功, 分类数={}", preferences.size());

            return Result.ok("获取偏好分类成功", data);
        } catch (Exception e) {
            log.error("获取偏好分类失败, userId={}", userId, e);
            return Result.fail(500, "服务异常");
        }
    }

    /**
     * 刷新热门商品缓存（管理员接口）
     *
     * 用于运维人员手动刷新热门商品缓存，立即生效
     *
     * 权限: 需要管理员权限（todo: 待补充权限验证）
     *
     * @return 刷新结果和时间戳
     *
     * 请求示例:
     * POST /api/v1/campus-taotao/goods/cache/refresh-hot
     */
    @PostMapping("/cache/refresh-hot")
    public Result refreshHotCache() {
        // TODO: 添加管理员权限检查
        // 当前实现略过权限检查，实际应该加入权限验证

        try {
            log.info("开始刷新热门商品缓存");

            recommendationService.refreshHotGoodsCache();

            Map<String, Object> data = new HashMap<>();
            data.put("refreshTime", java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            log.info("热门缓存刷新成功");

            return Result.ok("热门缓存刷新成功", data);
        } catch (Exception e) {
            log.error("刷新热门缓存失败", e);
            return Result.fail(500, "服务异常");
        }
    }

    /**
     * 获取推荐系统统计信息（监控用）
     *
     * 用于监控推荐系统的运行状态和缓存状况
     *
     * @return 推荐系统统计数据
     *
     * 请求示例:
     * GET /api/v1/campus-taotao/goods/statistics
     */
    @GetMapping("/statistics")
    public Result getRecommendationStatistics() {
        try {
            log.debug("获取推荐系统统计信息");

            RecommendationStatisticsDTO statistics = recommendationService.getRecommendationStatistics();

            log.debug("获取推荐系统统计信息成功, 状态={}", statistics.getStatus());

            return Result.ok("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取推荐系统统计信息失败", e);
            return Result.fail(500, "服务异常");
        }
    }

}
