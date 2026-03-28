package com.bit.scmu_taotao.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsEditRequest {
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotBlank(message = "商品描述不能为空")
    private String desc;

    private String remark;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    private String purpose;

    private String exchangeAddr;

    private List<String> imgUrls;

    @NotBlank(message = "商品类型不能为空")
    private String type; // sell/buy
}