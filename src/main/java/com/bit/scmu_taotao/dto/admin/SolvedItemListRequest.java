package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SolvedItemListRequest {
    @NotBlank(message = "type不能为空")
    private String type;
    private String status;
    private String keyword;
    @Min(value = 1, message = "page必须大于等于1")
    private Integer page = 1;
    @Min(value = 1, message = "size必须大于等于1")
    @Max(value = 50, message = "size不能超过50")
    private Integer size = 10;
}
