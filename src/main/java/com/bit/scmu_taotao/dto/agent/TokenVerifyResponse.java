package com.bit.scmu_taotao.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal API — Token 校验响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyResponse {
    /**
     * 用户 ID（学号）
     */
    private String userId;

    /**
     * 用户角色：USER / ADMIN
     */
    private String userRole;
}
