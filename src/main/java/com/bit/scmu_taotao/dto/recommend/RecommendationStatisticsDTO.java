package com.bit.scmu_taotao.dto.recommend;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐系统统计信息DTO - 用于展示推荐系统的运行统计数据
 *
 * 包含:
 * - 浏览记录统计
 * - 缓存状态
 * - 最后更新时间
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总浏览记录数
     */
    private Long totalBrowseRecords;

    /**
     * 今日新增浏览记录数
     */
    private Long todayBrowseRecords;

    /**
     * 活跃用户数（30天内有浏览）
     */
    private Long activeUsers;

    /**
     * 热门商品缓存是否有效
     */
    private Boolean hotCacheValid;

    /**
     * 热门商品缓存数量
     */
    private Integer hotCacheCount;

    /**
     * 热门缓存最后刷新时间
     */
    private LocalDateTime lastCacheRefreshTime;

    /**
     * 推荐系统状态
     * - "normal": 正常
     * - "degraded": 降级（缓存失效）
     * - "error": 异常
     */
    private String status;

    /**
     * 系统说明信息
     */
    private String message;
}

