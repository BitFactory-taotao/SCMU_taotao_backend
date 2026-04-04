package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.RecommendationConfig;
import com.bit.scmu_taotao.service.RecommendationConfigService;
import com.bit.scmu_taotao.mapper.RecommendationConfigMapper;
import com.bit.scmu_taotao.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 35314
 * @description 针对表【recommendation_config(推荐权重配置表)】的数据库操作Service实现
 * @createDate 2026-04-02 19:58:28
 */
@Slf4j
@Service
public class RecommendationConfigServiceImpl extends ServiceImpl<RecommendationConfigMapper, RecommendationConfig>
        implements RecommendationConfigService {

    @Autowired
    private RecommendationConfigMapper configMapper;

    @Autowired
    private RedisService redisService;

    private static final String CONFIG_CACHE_PREFIX = "recommendation:config:";
    private static final long CONFIG_CACHE_EXPIRE = 3600; // 1小时缓存
    private static final List<String> CONFIG_KEYS = List.of(
            "category_preference_weight",
            "favorite_weight",
            "view_count_weight",
            "recent_publish_bonus",
            "hot_cache_count",
            "hot_cache_expire_hours",
            "browse_history_days",
            "click_anti_spam_hours"
    );

    @Override
    public BigDecimal getConfigValue(String configKey) {
        // 1. 尝试从Redis缓存获取
        String cacheKey = CONFIG_CACHE_PREFIX + configKey;
        Object cachedValue = redisService.get(cacheKey);

        if (cachedValue != null) {
            return new BigDecimal(cachedValue.toString());
        }

        // 2. 从数据库查询
        BigDecimal value = configMapper.getConfigValue(configKey);

        // 3. 写入缓存
        if (value != null) {
            redisService.setWithExpire(cacheKey, value, CONFIG_CACHE_EXPIRE, TimeUnit.SECONDS);
        }

        return value;
    }

    @Override
    public void refreshConfigCache() {
        try {
            // RedisService 暂无 pattern 删除接口，按已知配置键清理缓存
            for (String key : CONFIG_KEYS) {
                redisService.delete(CONFIG_CACHE_PREFIX + key);
            }
            log.info("推荐配置缓存已刷新");
        } catch (Exception e) {
            log.error("刷新配置缓存失败", e);
        }
    }
}




