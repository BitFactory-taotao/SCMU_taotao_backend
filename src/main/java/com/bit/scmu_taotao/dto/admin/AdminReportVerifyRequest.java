package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 管理员-举报处理请求 DTO
 */
@Data
public class AdminReportVerifyRequest {

    // PASS（属实）或 REJECT（驳回）
    @NotBlank(message = "action不能为空")
    @Pattern(regexp = "^(PASS|REJECT)$", message = "action必须是PASS或REJECT")
    private String action;

    // PASS 时必传，扣分值大于0且不超过100
    @Min(value = 1, message = "deductScore必须大于0")
    @Max(value = 100, message = "deductScore不能超过100")
    private Integer deductScore;
}

