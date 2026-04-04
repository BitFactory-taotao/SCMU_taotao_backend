package com.bit.scmu_taotao.service.impl;

import com.bit.scmu_taotao.dto.recommend.BrowseRecordResponseDTO;
import com.bit.scmu_taotao.dto.recommend.PublisherInfoDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendGoodsDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendListResponseDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendationStatisticsDTO;
import com.bit.scmu_taotao.dto.recommend.UserCategoryPreferenceDTO;
import com.bit.scmu_taotao.entity.UserGoodsBrowse;
import com.bit.scmu_taotao.mapper.TGoodsMapper;
import com.bit.scmu_taotao.mapper.UserGoodsBrowseMapper;
import com.bit.scmu_taotao.service.RecommendationConfigService;
import com.bit.scmu_taotao.service.RecommendationService;
import com.bit.scmu_taotao.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    @Autowired
    private UserGoodsBrowseMapper userGoodsBrowseMapper;

    @Autowired
    private TGoodsMapper tGoodsMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RecommendationConfigService configService;

    private static final String HOT_GOODS_CACHE_KEY = "recommendation:hot_goods_cache";
    private static final String HOT_GOODS_CACHE_REFRESH_TIME_KEY = "recommendation:hot_goods_cache_refresh_time";

    @Override
    public RecommendListResponseDTO getRecommendations(String userId, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        RecommendListResponseDTO response = new RecommendListResponseDTO();
        response.setPage(safePage);
        response.setPageSize(safePageSize);

        // 1. 查询用户偏好分类
        List<String> topCategoryStrings = userGoodsBrowseMapper.getUserTop2Categories(userId);
        List<Integer> topCategories = topCategoryStrings == null
                ? new ArrayList<>()
                : topCategoryStrings.stream().map(Integer::valueOf).collect(Collectors.toList());

        if (!topCategories.isEmpty()) {
            // 2. 有浏览记录 -> 个性化推荐
            response.setRecommendType("personalized");
            List<Map<String, Object>> results = tGoodsMapper.getPersonalizedRecommendations(
                    userId,
                    topCategories,
                    configService.getConfigValue("category_preference_weight").doubleValue(),
                    configService.getConfigValue("favorite_weight").doubleValue(),
                    configService.getConfigValue("view_count_weight").doubleValue(),
                    configService.getConfigValue("recent_publish_bonus").doubleValue(),
                    safePageSize,
                    (long) (safePage - 1) * safePageSize
            );
            response.setList(mapToGoodsDTOList(results));
            response.setTotal((long) response.getList().size());
        } else {
            // 3. 无浏览记录 -> 冷启动推荐
            response.setRecommendType("coldstart");
            List<Map<String, Object>> results = getHotGoodsFromCache(safePageSize, safePage);
            response.setList(mapToGoodsDTOList(results));
            response.setTotal((long) Math.min(results.size(),
                    configService.getConfigValue("hot_cache_count").intValue()));
        }

        return response;
    }

    @Override
    public List<UserCategoryPreferenceDTO> getUserPreferenceCategories(String userId) {
        List<Map<String, Object>> stats = userGoodsBrowseMapper.getUserBrowseStatistics(userId);

        if (stats == null || stats.isEmpty()) {
            return new ArrayList<>();
        }

        int total = stats.stream().mapToInt(m -> ((Number) m.get("browse_count")).intValue()).sum();
        final int finalTotal = total;

        return stats.stream().map(m -> {
            UserCategoryPreferenceDTO dto = new UserCategoryPreferenceDTO();
            dto.setCategoryId(((Number) m.get("category_id")).intValue());
            dto.setCategoryName((String) m.get("category_name"));
            int browseCount = ((Number) m.get("browse_count")).intValue();
            dto.setBrowseCount(browseCount);
            dto.setPercentage(finalTotal > 0 ? (browseCount * 100.0 / finalTotal) : 0);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public BrowseRecordResponseDTO recordBrowseAndUpdateViewCount(String userId, Long goodsId) {
        BrowseRecordResponseDTO response = new BrowseRecordResponseDTO();

        try {
            // 1. 异步保存浏览记录
            UserGoodsBrowse browse = new UserGoodsBrowse();
            browse.setUserId(userId);
            browse.setGoodsId(goodsId);
            browse.setBrowseTime(LocalDateTime.now());
            // 调用 Service 或 Mapper 保存
            userGoodsBrowseMapper.insert(browse);

            // 2. 检查防刷规则（Redis缓存）
            String antiSpamKey = buildAntiSpamKey(userId, goodsId);
            boolean isClickWithinWindow = redisService.get(antiSpamKey) != null;

            if (!isClickWithinWindow) {
                // 3. 更新点击量
                tGoodsMapper.incrementViewCount(goodsId);
                log.info("用户 {} 浏览了商品 {}, 点击量已更新", userId, goodsId);

                // 4. 记录防刷标记（1小时过期）
                int antiSpamHours = configService.getConfigValue("click_anti_spam_hours").intValue();
                redisService.setWithExpire(antiSpamKey, "1", antiSpamHours, TimeUnit.HOURS);

                response.setIsClickCountUpdated(true);
            } else {
                response.setIsClickCountUpdated(false);
            }

            // 5. 获取当前点击量
            Integer viewCount = tGoodsMapper.getViewCount(goodsId);
            response.setCurrentViewCount(viewCount != null ? viewCount : 0);

        } catch (Exception e) {
            log.error("记录浏览或更新点击量失败, userId={}, goodsId={}", userId, goodsId, e);
            response.setIsClickCountUpdated(false);
            response.setCurrentViewCount(0);
        }

        return response;
    }

    @Override
    public void refreshHotGoodsCache() {
        try {
            int hotCacheCount = configService.getConfigValue("hot_cache_count").intValue();
            List<Map<String, Object>> hotGoods = tGoodsMapper.getColdStartRecommendations(hotCacheCount);

            int hotCacheExpireHours = configService.getConfigValue("hot_cache_expire_hours").intValue();
            redisService.setWithExpire(HOT_GOODS_CACHE_KEY, hotGoods, hotCacheExpireHours, TimeUnit.HOURS);

            // 记录缓存刷新时间
            redisService.setWithExpire(HOT_GOODS_CACHE_REFRESH_TIME_KEY,
                    LocalDateTime.now().toString(), hotCacheExpireHours, TimeUnit.HOURS);

            log.info("热门商品缓存已刷新, 数量={}, 有效期={}小时", hotGoods.size(), hotCacheExpireHours);
        } catch (Exception e) {
            log.error("刷新热门缓存失败", e);
        }
    }

    @Override
    public RecommendationStatisticsDTO getRecommendationStatistics() {
        RecommendationStatisticsDTO stats = new RecommendationStatisticsDTO();

        try {
            // 获取浏览记录统计（可选，如需要可调用Mapper）

            // 获取热门缓存状态
            Object cachedObj = redisService.get(HOT_GOODS_CACHE_KEY);
            boolean hotCacheValid = cachedObj != null && cachedObj instanceof List;

            stats.setHotCacheValid(hotCacheValid);
            if (hotCacheValid) {
                stats.setHotCacheCount(((List<?>) cachedObj).size());
            } else {
                stats.setHotCacheCount(0);
            }

            // 获取缓存刷新时间
            Object refreshTimeObj = redisService.get(HOT_GOODS_CACHE_REFRESH_TIME_KEY);
            if (refreshTimeObj != null) {
                try {
                    stats.setLastCacheRefreshTime(LocalDateTime.parse(refreshTimeObj.toString()));
                } catch (Exception e) {
                    log.warn("解析缓存刷新时间失败", e);
                }
            }

            // 设置系统状态
            if (hotCacheValid) {
                stats.setStatus("normal");
                stats.setMessage("推荐系统正常运行");
            } else {
                stats.setStatus("degraded");
                stats.setMessage("热门缓存失效，降级处理");
            }

        } catch (Exception e) {
            log.error("获取推荐系统统计信息失败", e);
            stats.setStatus("error");
            stats.setMessage("获取统计信息异常: " + e.getMessage());
        }

        return stats;
    }

    // ==================== 私有方法 ====================

    private List<RecommendGoodsDTO> mapToGoodsDTOList(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.stream().map(this::mapToGoodsDTO).collect(Collectors.toList());
    }

    private RecommendGoodsDTO mapToGoodsDTO(Map<String, Object> row) {
        RecommendGoodsDTO dto = new RecommendGoodsDTO();
        dto.setGoodsId(((Number) row.get("goods_id")).longValue());
        dto.setGoodsName((String) row.get("goods_name"));
        dto.setPrice((BigDecimal) row.get("price"));
        dto.setCategoryId(((Number) row.get("category_id")).intValue());
        dto.setCategoryName((String) row.get("category_name"));
        dto.setViewCount(((Number) row.get("view_count")).intValue());

        // 处理 is_favorited 可能的不同格式
        Object favoritedObj = row.get("is_favorited");
        if (favoritedObj instanceof Boolean) {
            dto.setIsFavorited((Boolean) favoritedObj);
        } else {
            dto.setIsFavorited(((Number) favoritedObj).intValue() == 1);
        }

        dto.setImageUrl((String) row.get("image_url"));
        dto.setCreateTime((String) row.get("create_time"));

        Object scoreObj = row.get("recommend_score");
        if (scoreObj == null) {
            scoreObj = row.get("hot_score");
        }
        if (scoreObj != null) {
            dto.setRecommendScore(((Number) scoreObj).doubleValue());
        }

        // 发布者信息
        PublisherInfoDTO publisher = new PublisherInfoDTO();
        publisher.setUserId((String) row.get("user_id"));
        publisher.setUserName((String) row.get("user_name"));
        publisher.setCreditStar(((Number) row.get("credit_star")).doubleValue());
        publisher.setCreditScore(((Number) row.get("credit_score")).intValue());
        dto.setPublisherInfo(publisher);

        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getHotGoodsFromCache(Integer pageSize, Integer page) {
        Object cachedObj = redisService.get(HOT_GOODS_CACHE_KEY);
        List<Map<String, Object>> allHotGoods;

        if (!(cachedObj instanceof List)) {
            // 缓存失效，重新查询
            int hotCacheCount = configService.getConfigValue("hot_cache_count").intValue();
            allHotGoods = tGoodsMapper.getColdStartRecommendations(hotCacheCount);
            int hotCacheExpireHours = configService.getConfigValue("hot_cache_expire_hours").intValue();
            redisService.setWithExpire(HOT_GOODS_CACHE_KEY, allHotGoods, hotCacheExpireHours, TimeUnit.HOURS);
        } else {
            allHotGoods = (List<Map<String, Object>>) cachedObj;
        }

        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allHotGoods.size());

        if (start >= allHotGoods.size()) {
            return new ArrayList<>();
        }

        return allHotGoods.subList(start, end);
    }

    private String buildAntiSpamKey(String userId, Long goodsId) {
        return String.format("recommendation:click_anti_spam:%s:%d", userId, goodsId);
    }
}
