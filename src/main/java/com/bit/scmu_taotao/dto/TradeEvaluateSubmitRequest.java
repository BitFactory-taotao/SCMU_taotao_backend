package com.bit.scmu_taotao.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class TradeEvaluateSubmitRequest {
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotBlank(message = "卖家ID不能为空")
    private String sellerId;

    @NotNull(message = "商品描述评分不能为空")
    @Min(value = 0, message = "商品描述评分必须在0-5之间")
    @Max(value = 5, message = "商品描述评分必须在0-5之间")
    private Integer goodsDescScore;

    @NotNull(message = "沟通流畅评分不能为空")
    @Min(value = 0, message = "沟通流畅评分必须在0-5之间")
    @Max(value = 5, message = "沟通流畅评分必须在0-5之间")
    private Integer communicateScore;

    @NotNull(message = "总体评价评分不能为空")
    @Min(value = 0, message = "总体评价评分必须在0-5之间")
    @Max(value = 5, message = "总体评价评分必须在0-5之间")
    private Integer totalScore;

    @Size(max = 1000, message = "评价内容不能超过1000字")
    private String content;

    @Size(max = 9, message = "评价图片最多9张")
    private List<String> imgUrls;

    @NotNull(message = "匿名标识不能为空")
    private Boolean isAnonymous;
}
