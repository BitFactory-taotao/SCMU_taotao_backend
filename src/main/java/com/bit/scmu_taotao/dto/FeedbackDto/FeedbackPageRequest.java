package com.bit.scmu_taotao.dto.FeedbackDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackPageRequest {
    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "page必须大于等于1")
    private Integer page = 1;

    @NotNull(message = "size不能为空")
    @Min(value = 1, message = "size必须大于等于1")
    private Integer size = 10;
}

