package com.bit.scmu_taotao.dto.recommend;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 推荐商品DTO - 用于前端展示的商品信息
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendGoodsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须大于0")
    private Long goodsId;

    /**
     * 商品名称
     */
    @NotNull(message = "商品名称不能为空")
    private String goodsName;

    /**
     * 商品价格
     */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "商品价格必须大于0")
    private BigDecimal price;

    /**
     * 分类ID
     */
    @Min(value = 1, message = "分类ID必须大于0")
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 点击量（浏览次数）
     */
    @Min(value = 0, message = "点击量不能为负数")
    private Integer viewCount;

    /**
     * 是否被当前用户收藏
     */
    @JsonProperty("isFavorited")
    private Boolean isFavorited;

    /**
     * 商品主图URL
     */
    private String imageUrl;

    /**
     * 发布时间 (格式: yyyy-MM-dd HH:mm:ss)
     */
    private String createTime;

    /**
     * 推荐得分（用于排序，可选展示）
     * 范围: 0-100+
     */
    private Double recommendScore;

    /**
     * 发布者信息
     */
    private PublisherInfoDTO publisherInfo;
}

