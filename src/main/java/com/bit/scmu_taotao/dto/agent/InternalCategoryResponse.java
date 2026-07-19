package com.bit.scmu_taotao.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal API — 商品分类响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalCategoryResponse {
    private Integer categoryId;
    private String categoryName;
    private Integer sort;
}
