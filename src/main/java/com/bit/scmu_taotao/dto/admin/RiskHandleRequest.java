package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 管理员-风险账号处理请求 DTO
 */
@Data
public class RiskHandleRequest {

    @NotEmpty(message = "userIds不能为空")
    private List<String> userIds;

    @NotBlank(message = "action不能为空")
    @Pattern(regexp = "^(BAN|CLEAR)$", message = "action必须是BAN或CLEAR")
    private String action;

    // 查封原因，可选
    private String reason;
}

