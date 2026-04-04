package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.entity.RecommendationConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

/**
* @author 35314
* @description 针对表【recommendation_config(推荐权重配置表)】的数据库操作Service
* @createDate 2026-04-02 19:58:28
*/
public interface RecommendationConfigService extends IService<RecommendationConfig> {
    /**
     * 获取配置值（带缓存）
     */
    BigDecimal getConfigValue(String configKey);

    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();
}
