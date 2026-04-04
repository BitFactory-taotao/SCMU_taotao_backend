package com.bit.scmu_taotao.dto.recommend;

import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;

/**
 * 用户分类偏好DTO - 用户浏览偏好分析
 *
 * 用于展示用户在各个商品分类中的浏览统计数据
 * 主要用于Debug和用户分析场景
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCategoryPreferenceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品分类ID
     */
    @NotNull(message = "分类ID不能为空")
    @Min(value = 1, message = "分类ID必须大于0")
    private Integer categoryId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度应在1-50之间")
    private String categoryName;

    /**
     * 用户在该分类中的浏览次数
     * 统计周期: 近30天
     */
    @Min(value = 0, message = "浏览次数不能为负数")
    private Integer browseCount;

    /**
     * 该分类在用户所有浏览中的占比百分比 (0-100)
     * 计算公式: (本分类浏览数 / 总浏览数) * 100
     */
    @DecimalMin(value = "0.0", message = "占比不能为负数")
    @DecimalMax(value = "100.0", message = "占比不能超过100")
    private Double percentage;

    /**
     * 判断该分类是否为用户的主要偏好
     * 标准: 占比 >= 20%
     */
    public boolean isPrimaryPreference() {
        return percentage != null && percentage >= 20.0;
    }

    /**
     * 获取偏好等级描述
     */
    public String getPreferenceLevel() {
        if (percentage == null) {
            return "未知";
        }
        if (percentage >= 40.0) {
            return "非常热门";
        } else if (percentage >= 20.0) {
            return "热门";
        } else if (percentage >= 10.0) {
            return "一般";
        } else {
            return "较少浏览";
        }
    }
}

