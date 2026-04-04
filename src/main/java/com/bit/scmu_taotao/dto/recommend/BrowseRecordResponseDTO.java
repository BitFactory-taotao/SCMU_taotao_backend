package com.bit.scmu_taotao.dto.recommend;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

/**
 * 浏览记录响应DTO - 记录浏览并更新点击量后的结果
 *
 * 返回信息:
 * 1. 本次请求是否有效更新了点击量（防刷判断）
 * 2. 商品当前的点击量统计
 *
 * @author 推荐系统
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrowseRecordResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本次是否有效更新了点击量
     *
     * true: 点击量已增加（符合防刷规则）
     * false: 点击量未增加（被防刷机制拦截或其他原因）
     *
     * 防刷规则: 同一用户1小时内对同一商品仅计1次点击
     */
    @NotNull(message = "点击量更新标志不能为空")
    private Boolean isClickCountUpdated;

    /**
     * 该商品当前的点击量（浏览次数总数）
     *
     * 范围: >= 0
     * 说明: 由于防刷机制，当前值与历史累积值一致
     */
    @NotNull(message = "当前点击量不能为空")
    @Min(value = 0, message = "当前点击量不能为负数")
    private Integer currentViewCount;

    /**
     * 判断是否被防刷拦截
     */
    public boolean isBlocked() {
        return !isClickCountUpdated;
    }
}

