package com.bit.scmu_taotao.dto.recommend;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.io.Serializable;

/**
 * 推荐查询请求DTO - 前端查询推荐商品的请求参数
 *
 * 用于分页获取推荐商品列表，自动识别用户身份和推荐类型
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     * 默认值: 1
     */
    @Min(value = 1, message = "页码必须从1开始")
    private Integer page = 1;

    /**
     * 每页数量（分页大小）
     * 默认值: 10
     * 最大值: 100（防止过大的查询）
     */
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;

    /**
     * 推荐类型过滤（可选）
     *
     * - null/empty: 自动判断（推荐）
     * - "personalized": 只获取个性化推荐
     * - "coldstart": 只获取冷启动推荐
     *
     * 说明: 不传递此参数时，系统根据用户浏览历史自动决策
     */
    private String recommendType;

    /**
     * 分类ID过滤（可选）
     * 当指定时，只返回该分类下的推荐商品
     */
    @Min(value = 1, message = "分类ID必须大于0")
    private Integer categoryId;

    /**
     * 验证请求参数的有效性
     */
    public boolean isValid() {
        return page != null && page >= 1 &&
               pageSize != null && pageSize > 0 && pageSize <= 100;
    }

    /**
     * 安全化页码和每页数量
     */
    public void normalize() {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
    }

    /**
     * 计算分页偏移量（用于SQL OFFSET）
     */
    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }
}

