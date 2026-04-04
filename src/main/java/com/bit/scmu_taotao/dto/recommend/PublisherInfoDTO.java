package com.bit.scmu_taotao.dto.recommend;

import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发布者信息DTO - 商品发布者的基本信息
 *
 * 用于展示商品发布者的信誉等级、信誉分等信息
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublisherInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（学号/工号）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 用户真实姓名
     */
    @NotBlank(message = "用户姓名不能为空")
    @Size(min = 1, max = 50, message = "用户姓名长度应在1-50之间")
    private String userName;

    /**
     * 信誉星级 (0-5.0)
     * 小数点保留1位
     */
    @DecimalMin(value = "0.0", message = "信誉星级不能为负数")
    @DecimalMax(value = "5.0", message = "信誉星级不能超过5.0")
    private Double creditStar;

    /**
     * 信誉分数（初始100分，扣分制）
     * 范围: 0-100
     */
    @Min(value = 0, message = "信誉分数不能为负数")
    @Max(value = 100, message = "信誉分数不能超过100")
    private Integer creditScore;

    /**
     * 判断用户是否为高信誉用户
     * 标准: 信誉星级 >= 4.0
     */
    public boolean isHighCredit() {
        return creditStar != null && creditStar >= 4.0;
    }

    /**
     * 获取信誉等级文本
     */
    public String getCreditLevel() {
        if (creditStar == null) {
            return "未知";
        }
        if (creditStar >= 4.5) {
            return "优秀";
        } else if (creditStar >= 4.0) {
            return "良好";
        } else if (creditStar >= 3.0) {
            return "一般";
        } else {
            return "较差";
        }
    }
}

