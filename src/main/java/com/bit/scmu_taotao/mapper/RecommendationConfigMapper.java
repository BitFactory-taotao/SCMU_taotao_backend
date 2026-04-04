package com.bit.scmu_taotao.mapper;

import com.bit.scmu_taotao.entity.RecommendationConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * @author 35314
 * @description 针对表【recommendation_config(推荐权重配置表)】的数据库操作Mapper
 * @createDate 2026-04-02 19:58:28
 * @Entity com.bit.scmu_taotao.entity.RecommendationConfig
 */
public interface RecommendationConfigMapper extends BaseMapper<RecommendationConfig> {
    /**
     * 根据配置键获取配置值
     */
    @Select("SELECT config_value FROM recommendation_config WHERE config_key = #{configKey} LIMIT 1")
    BigDecimal getConfigValue(@Param("configKey") String configKey);


}




