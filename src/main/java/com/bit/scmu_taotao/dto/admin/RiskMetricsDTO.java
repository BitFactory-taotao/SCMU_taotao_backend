package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员-风险账号多维详情 DTO
 */
@Data
public class RiskMetricsDTO {

    // 学号
    private String userId;

    // 姓名
    private String userName;

    // 头像
    private String avatar;

    // 信誉分
    private Integer creditScore;

    // 信誉星级
    private BigDecimal creditStar;

    // 风险指标统计
    private Metrics metrics;

    /**
     * 风险指标统计项
     */
    @Data
    public static class Metrics {
        private Long reportCount;
        private Long itemViolationCount;
        private Long blacklistCount;
        private Long langViolationCount;
        private Long lowRatingCount;
    }
}

