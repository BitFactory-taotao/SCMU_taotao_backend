package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminFeedbackPageRequest {

    private String keyword;

    // pending / processed
    private String status;

    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "page必须大于等于1")
    private Integer page = 1;

    @NotNull(message = "size不能为空")
    @Min(value = 1, message = "size必须大于等于1")
    @Max(value = 50, message = "size不能超过50")
    private Integer size = 10;
}
