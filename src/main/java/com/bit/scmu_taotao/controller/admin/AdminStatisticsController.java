package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.service.AdminDashboardService;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员工作台统计 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    /**
     * 获取工作台概览统计
     *
     * 统计数据包括：
     * - 待审核商品数
     * - 风险用户数
     * - 待处理反馈数
     * - 待处理举报数
     * - 已解决事项总数
     *
     * 并计算每项的日同比趋势
     */
    @GetMapping("/overview")
    public Result overview() {
        log.info("管理员查询工作台统计");
        return adminDashboardService.getOverviewStatistics();
    }
}

