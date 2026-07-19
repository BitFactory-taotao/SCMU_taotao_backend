package com.bit.scmu_taotao.dto.agent;

import lombok.Data;

/**
 * Internal API — Token 校验请求
 */
@Data
public class TokenVerifyRequest {
    /**
     * 用户 Token（Bearer 前缀可选）
     */
    private String token;
}
