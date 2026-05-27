package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员-举报列表查询请求 DTO
 */
@Data
public class AdminReportPageRequest {

    // 搜索被举报人姓名或学号，可选
    private String keyword;

    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "page必须大于等于1")
    private Integer page = 1;

    @NotNull(message = "size不能为空")
    @Min(value = 1, message = "size必须大于等于1")
    @Max(value = 50, message = "size不能超过50")
    private Integer size = 10;
}

