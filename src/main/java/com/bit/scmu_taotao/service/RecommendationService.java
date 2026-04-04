package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.recommend.BrowseRecordResponseDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendListResponseDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendationStatisticsDTO;
import com.bit.scmu_taotao.dto.recommend.UserCategoryPreferenceDTO;

import java.util.List;

/**
 * 推荐服务接口
 * 核心功能:
 * 1. 推荐商品列表 - 支持个性化推荐和冷启动推荐
 * 2. 用户偏好分析 - 查询用户的分类偏好
 * 3. 浏览记录埋点 - 记录用户浏览并更新点击量（含防刷）
 * 4. 缓存管理 - 刷新热门商品缓存
 */
public interface RecommendationService {

    /**
     * 获取推荐商品列表（自动识别个性化/冷启动）
     * 流程:
     * 1. 检查用户是否有浏览历史
     * 2. 有浏览记录 → 查询用户Top2偏好分类，进行个性化推荐
     * 3. 无浏览记录 → 返回全校热门Top50商品（冷启动推荐）
     *
     * @param userId 用户ID（学号）
     * @param page 页码（从1开始）
     * @param pageSize 每页数量（1-100）
     * @return 推荐结果（包含推荐类型和商品列表）
     */
    RecommendListResponseDTO getRecommendations(String userId, Integer page, Integer pageSize);

    /**
     * 获取用户偏好分类Top2（用于调试和分析）
     * 统计用户近30天浏览数最多的两个商品分类，用于个性化推荐决策
     *
     * @param userId 用户ID
     * @return 用户偏好分类列表（按浏览数降序，最多2条）
     */
    List<UserCategoryPreferenceDTO> getUserPreferenceCategories(String userId);

    /**
     * 记录浏览并更新点击量（含防刷机制）
     * 功能:
     * 1. 保存浏览记录到数据库
     * 2. 检查防刷规则（1小时内同一商品仅计1次点击）
     * 3. 若符合规则，增加商品点击量
     * 4. 返回更新结果和当前点击量
     *
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @return 浏览记录响应（点击量是否更新 + 当前点击量）
     */
    BrowseRecordResponseDTO recordBrowseAndUpdateViewCount(String userId, Long goodsId);

    /**
     * 刷新热门商品缓存（运维接口）
     * 功能:
     * 1. 查询冷启动推荐商品（全校热门Top50）
     * 2. 缓存到Redis（过期时间1小时）
     * 3. 用于降低数据库查询压力
     * 触发时机:
     * - 系统启动时（自动）
     * - 每小时定时刷新（定时任务）
     * - 手动调用（运维接口）
     */
    void refreshHotGoodsCache();

    /**
     * 获取推荐系统统计信息（可选接口）
     * 用于监控和统计推荐系统的运行状态
     *
     * @return 推荐系统统计数据
     */
    RecommendationStatisticsDTO getRecommendationStatistics();
}
