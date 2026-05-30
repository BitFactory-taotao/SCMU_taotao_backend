package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.util.common.Result;

/**
 * 管理员工作台服务接口
 */
public interface AdminDashboardService {

    /**
     * 获取工作台概览统计
     *
     * @return 概览统计数据
     */
    Result getOverviewStatistics();
}

