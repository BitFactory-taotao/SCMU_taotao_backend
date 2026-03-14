package com.bit.scmu_taotao.dto;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {
    
    /**
     * 用户 ID（学号/工号）
     */
    private String userId;
    
    /**
     * 密码
     */
    private String password;
}
