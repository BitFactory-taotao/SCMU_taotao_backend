package com.bit.scmu_taotao.service.impl;

import com.bit.scmu_taotao.mapper.UserGoodsBrowseMapper;
import com.bit.scmu_taotao.service.RecommendationConfigService;
import com.bit.scmu_taotao.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 推荐系统定时任务服务
 *
 * 负责:
 * 1. 系统启动时预热热门商品缓存
 * 2. 每日凌晨清理历史浏览记录
 * 3. 定时刷新热门商品缓存
 *
 * @author 推荐系统
 * @version 1.0
 */
@Component
@Slf4j
public class RecommendationScheduleService {

    private final UserGoodsBrowseMapper userGoodsBrowseMapper;
    private final RecommendationService recommendationService;
    private final RecommendationConfigService configService;

    public RecommendationScheduleService(UserGoodsBrowseMapper userGoodsBrowseMapper,
                                         RecommendationService recommendationService,
                                         RecommendationConfigService configService) {
        this.userGoodsBrowseMapper = userGoodsBrowseMapper;
        this.recommendationService = recommendationService;
        this.configService = configService;
    }

    /**
     * 应用启动时预热缓存
     * 
     * 在系统启动时自动加载热门商品到缓存，避免首次请求时的数据库查询延迟
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCache() {
        try {
            log.info("开始预热推荐系统缓存...");
            recommendationService.refreshHotGoodsCache();
            log.info("推荐系统缓存预热完成");
        } catch (Exception e) {
            log.error("推荐系统缓存预热失败", e);
        }
    }

    /**
     * 每日凌晨清理历史浏览记录，防止数据表持续膨胀。
     * 
     * 执行时间: 每日凌晨 02:00:00
     * 清理规则: 删除30天前的用户浏览记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupBrowseHistory() {
        try {
            int keepDays = getConfigOrDefault("browse_history_days", 30);
            long startTime = System.currentTimeMillis();
            
            int affected = userGoodsBrowseMapper.cleanupOldRecords(keepDays);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("浏览记录清理完成, keepDays={}, affectedRows={}, duration={}ms", 
                    keepDays, affected, duration);
        } catch (Exception e) {
            log.error("浏览记录清理任务失败", e);
        }
    }

    /**
     * 每小时刷新热门缓存，平衡实时性和查询压力。
     * 
     * 执行时间: 每小时第5分钟（:05）
     * 作用: 保持热门商品排序最新，降低数据库查询频率
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void refreshHotGoodsCache() {
        try {
            long startTime = System.currentTimeMillis();
            recommendationService.refreshHotGoodsCache();
            long duration = System.currentTimeMillis() - startTime;
            log.info("热门缓存刷新完成, duration={}ms", duration);
        } catch (Exception e) {
            log.error("热门缓存刷新任务失败", e);
        }
    }

    /**
     * 从配置获取值，如果获取失败则返回默认值
     */
    private int getConfigOrDefault(String key, int defaultValue) {
        try {
            BigDecimal value = configService.getConfigValue(key);
            return value == null ? defaultValue : value.intValue();
        } catch (Exception e) {
            log.warn("从配置获取{}失败，使用默认值{}", key, defaultValue, e);
            return defaultValue;
        }
    }
}

