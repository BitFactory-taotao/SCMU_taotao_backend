package com.bit.scmu_taotao.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员工作台概览统计 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsOverviewDTO {

    /**
     * 待审核商品
     */
    private Metric pendingGoods;

    /**
     * 风险用户
     */
    private Metric riskUsers;

    /**
     * 待处理反馈
     */
    private Metric pendingFeedback;

    /**
     * 待处理举报
     */
    private Metric pendingReports;

    /**
     * 已解决事项
     */
    private Metric solvedItems;

    /**
     * 数据快照时间
     */
    private String snapshotTime;

    /**
     * 内嵌指标统计类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metric {
        /**
         * 数量
         */
        private long count;

        /**
         * 趋势百分比（日同比）
         */
        private String trend;
    }
}

