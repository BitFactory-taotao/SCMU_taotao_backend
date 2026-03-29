package com.bit.scmu_taotao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlacklistPageQuery {
    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "page必须大于等于1")
    private Integer page = 1;

    @NotNull(message = "size不能为空")
    @Min(value = 1, message = "size必须大于等于1")
    @Max(value = 100, message = "size不能超过100")
    private Integer size = 10;
}