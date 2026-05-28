package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

/**
 * 管理员-风险用户列表项返回 DTO（风险待办队列）
 */
@Data
public class RiskUserListItemDTO {

    // 学号
    private String userId;

    // 姓名
    private String userName;

    // 头像
    private String avatar;

    // 风险等级
    private String riskLevel;

    // 注册时间，格式：yyyy-MM-dd HH:mm:ss
    private String registerTime;
}

