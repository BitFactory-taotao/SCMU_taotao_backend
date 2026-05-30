package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.admin.AdminStatisticsOverviewDTO;
import com.bit.scmu_taotao.dto.admin.SolvedItemCountDTO;
import com.bit.scmu_taotao.entity.TFeedback;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.entity.TUserReport;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.bit.scmu_taotao.util.common.KeyDescription.*;

/**
 * 管理员工作台服务实现
 */
@Slf4j
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 趋势指标 field 名称常量 */
    private static final String FIELD_PENDING_GOODS = "pending_goods";
    private static final String FIELD_RISK_USERS = "risk_users";
    private static final String FIELD_PENDING_FEEDBACK = "pending_feedback";
    private static final String FIELD_PENDING_REPORTS = "pending_reports";
    private static final String FIELD_SOLVED_ITEMS = "solved_items";

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TFeedbackService tFeedbackService;

    @Autowired
    private TUserReportService tUserReportService;

    @Autowired
    private AdminSolvedItemsService adminSolvedItemsService;

    @Autowired
    private RedisService redisService;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public Result getOverviewStatistics() {
        try {
            // 尝试从缓存读取
            String cacheData = (String) redisService.get(CACHE_KEY_OVERVIEW);
            if (cacheData != null) {
                log.debug("工作台统计命中缓存");
                AdminStatisticsOverviewDTO cachedDto = parseFromCacheOrNull(cacheData);
                if (cachedDto != null) {
                    cachedDto.setSnapshotTime(LocalDateTime.now().format(DATETIME_FORMATTER));
                    return Result.ok("获取工作台统计成功", cachedDto);
                }
            }

            // 缓存未命中，重新计算
            AdminStatisticsOverviewDTO overviewDTO = buildOverview();

            // 写回缓存
            try {
                String jsonStr = convertToJsonString(overviewDTO);
                redisService.setWithExpire(CACHE_KEY_OVERVIEW, jsonStr, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("缓存工作台统计失败：{}", e.getMessage());
            }

            return Result.ok("获取工作台统计成功", overviewDTO);
        } catch (Exception e) {
            log.error("查询工作台统计失败：{}", e.getMessage(), e);
            return Result.fail(500, "查询工作台统计失败，请稍后重试");
        }
    }

    /**
     * 构建完整的概览 DTO
     */
    private AdminStatisticsOverviewDTO buildOverview() {
        // 第一层：计算当前各项指标
        AdminStatisticsOverviewDTO.Metric pendingGoodsMetric = new AdminStatisticsOverviewDTO.Metric();
        AdminStatisticsOverviewDTO.Metric riskUsersMetric = new AdminStatisticsOverviewDTO.Metric();
        AdminStatisticsOverviewDTO.Metric pendingFeedbackMetric = new AdminStatisticsOverviewDTO.Metric();
        AdminStatisticsOverviewDTO.Metric pendingReportsMetric = new AdminStatisticsOverviewDTO.Metric();
        AdminStatisticsOverviewDTO.Metric solvedItemsMetric = new AdminStatisticsOverviewDTO.Metric();

        buildCurrentMetrics(pendingGoodsMetric, riskUsersMetric, pendingFeedbackMetric,
                pendingReportsMetric, solvedItemsMetric);

        // 第二层：批量附加趋势数据（一次 Redis 读 + 一次 Redis 写）
        Map<String, AdminStatisticsOverviewDTO.Metric> metricsMap = new HashMap<>();
        metricsMap.put(FIELD_PENDING_GOODS, pendingGoodsMetric);
        metricsMap.put(FIELD_RISK_USERS, riskUsersMetric);
        metricsMap.put(FIELD_PENDING_FEEDBACK, pendingFeedbackMetric);
        metricsMap.put(FIELD_PENDING_REPORTS, pendingReportsMetric);
        metricsMap.put(FIELD_SOLVED_ITEMS, solvedItemsMetric);
        attachAllTrends(metricsMap);

        // 第三层：组装最终 DTO
        return buildOverviewDTO(pendingGoodsMetric, riskUsersMetric, pendingFeedbackMetric,
                pendingReportsMetric, solvedItemsMetric);
    }

    /**
     * 计算当前各项指标数量
     */
    private void buildCurrentMetrics(
            AdminStatisticsOverviewDTO.Metric pendingGoodsMetric,
            AdminStatisticsOverviewDTO.Metric riskUsersMetric,
            AdminStatisticsOverviewDTO.Metric pendingFeedbackMetric,
            AdminStatisticsOverviewDTO.Metric pendingReportsMetric,
            AdminStatisticsOverviewDTO.Metric solvedItemsMetric) {

        try {
            // 待审核商品：isAudited=0 且 isDelete=0
            long pendingGoodsCount = tGoodsService.count(new LambdaQueryWrapper<TGoods>()
                    .eq(TGoods::getIsAudited, 0)
                    .eq(TGoods::getIsDelete, 0));
            pendingGoodsMetric.setCount(pendingGoodsCount);
            log.debug("待审核商品数：{}", pendingGoodsCount);

            // 风险用户：status=2（有风险需审核）且 creditScore<70 且 isDelete=0
            long riskUsersCount = tUserService.count(new LambdaQueryWrapper<TUser>()
                    .eq(TUser::getStatus, 2)
                    .lt(TUser::getCreditScore, 70)
                    .eq(TUser::getIsDelete, 0));
            riskUsersMetric.setCount(riskUsersCount);
            log.debug("风险用户数：{}", riskUsersCount);

            // 待处理反馈：feedbackStatus=0 且 isDelete=0
            long pendingFeedbackCount = tFeedbackService.count(new LambdaQueryWrapper<TFeedback>()
                    .eq(TFeedback::getFeedbackStatus, 0)
                    .eq(TFeedback::getIsDelete, 0));
            pendingFeedbackMetric.setCount(pendingFeedbackCount);
            log.debug("待处理反馈数：{}", pendingFeedbackCount);

            // 待处理举报：status=0（待审核）
            long pendingReportsCount = tUserReportService.count(new LambdaQueryWrapper<TUserReport>()
                    .eq(TUserReport::getStatus, 0));
            pendingReportsMetric.setCount(pendingReportsCount);
            log.debug("待处理举报数：{}", pendingReportsCount);

            // 已解决事项：复用现有接口
            long solvedItemsCount = getSolvedItemsCount();
            solvedItemsMetric.setCount(solvedItemsCount);
            log.debug("已解决事项数：{}", solvedItemsCount);

        } catch (Exception e) {
            log.error("计算当前指标失败：{}", e.getMessage(), e);
            pendingGoodsMetric.setCount(0L);
            riskUsersMetric.setCount(0L);
            pendingFeedbackMetric.setCount(0L);
            pendingReportsMetric.setCount(0L);
            solvedItemsMetric.setCount(0L);
        }
    }

    /**
     * 从已解决事项服务获取总数
     */
    private long getSolvedItemsCount() {
        try {
            Result result = adminSolvedItemsService.getSolvedItemCount();
            if (result != null && result.getCode() == 200 && result.getData() instanceof SolvedItemCountDTO) {
                SolvedItemCountDTO dto = (SolvedItemCountDTO) result.getData();
                return dto.getTotalCount();
            }
        } catch (Exception e) {
            log.warn("获取已解决事项总数失败：{}", e.getMessage());
        }
        return 0L;
    }

    /**
     * 批量附加趋势数据（日同比百分比）
     * 使用 Redis Hash 结构，一次读取昨日快照 + 一次写入今日快照
     */
    private void attachAllTrends(Map<String, AdminStatisticsOverviewDTO.Metric> metricsMap) {
        try {
            // 1. 一次读取昨日全部快照
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String yesterdayKey = TREND_KEY_PREFIX + yesterday.format(DATE_FORMATTER);
            Map<Object, Object> yesterdayData = redisService.hGetAll(yesterdayKey);

            // 2. 计算每个指标的趋势
            for (Map.Entry<String, AdminStatisticsOverviewDTO.Metric> entry : metricsMap.entrySet()) {
                String field = entry.getKey();
                AdminStatisticsOverviewDTO.Metric metric = entry.getValue();
                long todayCount = metric.getCount();

                long yesterdayCount = 0L;
                Object raw = yesterdayData.get(field);
                if (raw != null) {
                    try {
                        yesterdayCount = Long.parseLong(String.valueOf(raw));
                    } catch (NumberFormatException e) {
                        log.debug("解析昨日趋势值失败，field：{}，value：{}", field, raw);
                    }
                }

                String trend = calculateTrendPercent(todayCount, yesterdayCount);
                metric.setTrend(trend);
                log.debug("指标 {} 的趋势：{}", field, trend);
            }

            // 3. 一次写入今日全部快照
            LocalDate today = LocalDate.now();
            String todayKey = TREND_KEY_PREFIX + today.format(DATE_FORMATTER);
            Map<String, String> todayData = new HashMap<>();
            for (Map.Entry<String, AdminStatisticsOverviewDTO.Metric> entry : metricsMap.entrySet()) {
                todayData.put(entry.getKey(), String.valueOf(entry.getValue().getCount()));
            }
            redisService.hPutAll(todayKey, todayData);
            redisService.setExpire(todayKey, TREND_TTL_DAYS, TimeUnit.DAYS);

        } catch (Exception e) {
            log.warn("批量计算趋势失败：{}", e.getMessage());
            // 降级：所有指标趋势设为 0%
            for (AdminStatisticsOverviewDTO.Metric metric : metricsMap.values()) {
                metric.setTrend("0%");
            }
        }
    }

    /**
     * 计算趋势百分比（日同比）
     * 规则：
     * - yesterday == 0 && today == 0 → 0%
     * - yesterday == 0 && today > 0 → +100%
     * - 其他情况：(today - yesterday) / yesterday * 100，四舍五入到整数
     */
    private String calculateTrendPercent(long today, long yesterday) {
        if (yesterday == 0 && today == 0) {
            return "0%";
        }
        if (yesterday == 0 && today > 0) {
            return "+100%";
        }

        double percentChange = (double) (today - yesterday) / yesterday * 100;
        long roundedPercent = Math.round(percentChange);

        if (roundedPercent > 0) {
            return "+" + roundedPercent + "%";
        } else if (roundedPercent < 0) {
            return roundedPercent + "%";
        } else {
            return "0%";
        }
    }

    /**
     * 组装最终概览 DTO
     */
    private AdminStatisticsOverviewDTO buildOverviewDTO(
            AdminStatisticsOverviewDTO.Metric pendingGoods,
            AdminStatisticsOverviewDTO.Metric riskUsers,
            AdminStatisticsOverviewDTO.Metric pendingFeedback,
            AdminStatisticsOverviewDTO.Metric pendingReports,
            AdminStatisticsOverviewDTO.Metric solvedItems) {

        AdminStatisticsOverviewDTO dto = new AdminStatisticsOverviewDTO();
        dto.setPendingGoods(pendingGoods);
        dto.setRiskUsers(riskUsers);
        dto.setPendingFeedback(pendingFeedback);
        dto.setPendingReports(pendingReports);
        dto.setSolvedItems(solvedItems);
        dto.setSnapshotTime(LocalDateTime.now().format(DATETIME_FORMATTER));
        return dto;
    }

    /**
     * 将 DTO 转换为 JSON 字符串
     */
    private String convertToJsonString(AdminStatisticsOverviewDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.warn("DTO 转 JSON 字符串失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从缓存字符串解析 DTO
     */
    private AdminStatisticsOverviewDTO parseFromCacheOrNull(String cacheData) {
        try {
            return objectMapper.readValue(cacheData, AdminStatisticsOverviewDTO.class);
        } catch (Exception e) {
            log.debug("解析缓存数据失败：{}", e.getMessage());
            return null;
        }
    }
}