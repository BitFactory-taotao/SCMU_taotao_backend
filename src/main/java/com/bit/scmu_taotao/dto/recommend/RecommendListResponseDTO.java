package com.bit.scmu_taotao.dto.recommend;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 推荐列表响应DTO - 推荐结果分页返回格式
 *
 * 支持两种推荐模式:
 * 1. personalized - 个性化推荐（基于用户浏览历史和偏好分类）
 * 2. coldstart - 冷启动推荐（全校热门商品）
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendListResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从1开始")
    private Integer page;

    /**
     * 每页数量
     */
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    @jakarta.validation.constraints.Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize;

    /**
     * 推荐结果总数
     */
    @Min(value = 0, message = "总数不能为负数")
    private Long total;

    /**
     * 推荐类型
     * - personalized: 个性化推荐（用户有浏览历史）
     * - coldstart: 冷启动推荐（新用户或无浏览历史）
     */
    @NotNull(message = "推荐类型不能为空")
    private String recommendType;

    /**
     * 推荐商品列表
     */
    private List<RecommendGoodsDTO> list;

    /**
     * 验证推荐类型是否有效
     */
    public boolean isValidRecommendType() {
        return recommendType != null &&
               (recommendType.equals("personalized") || recommendType.equals("coldstart"));
    }
}

