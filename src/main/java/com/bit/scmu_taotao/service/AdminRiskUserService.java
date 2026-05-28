package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.admin.RiskHandleRequest;
import com.bit.scmu_taotao.dto.admin.RiskUserPageRequest;
import com.bit.scmu_taotao.util.common.Result;

/**
 * 管理员-风险账号审核服务
 */
public interface AdminRiskUserService {

    /**
     * 查询风险用户待办列表
     * @param request 查询条件
     * @return 风险用户列表
     */
    Result getRiskUserList(RiskUserPageRequest request);

    /**
     * 查询风险账号多维指标详情
     * @param userId 用户ID
     * @return 风险指标详情
     */
    Result getRiskMetrics(String userId);

    /**
     * 查封或消除风险账号
     * @param request 处理请求
     * @return 处理结果
     */
    Result handleRiskUsers(RiskHandleRequest request);
}

