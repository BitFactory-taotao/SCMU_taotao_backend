package com.bit.scmu_taotao.dto.agent;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Internal API — 商品增强搜索请求
 */
@Data
public class InternalGoodsSearchRequest {
    /**
     * 搜索关键词（可选，不传则返回全部）
     */
    private String keyword;

    /**
     * 分类 ID（可选）
     */
    private Integer categoryId;

    /**
     * 最低价格（可选）
     */
    private BigDecimal minPrice;

    /**
     * 最高价格（可选）
     */
    private BigDecimal maxPrice;

    /**
     * 页码，默认 1
     */
    private Integer page = 1;

    /**
     * 每页条数，默认 10，上限 50
     */
    private Integer size = 10;
}
