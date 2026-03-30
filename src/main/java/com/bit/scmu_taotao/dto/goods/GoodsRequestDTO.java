package com.bit.scmu_taotao.dto.goods;

import lombok.Data;
import java.util.List;

/**
 * 发布商品请求DTO
 */
@Data
public class GoodsRequestDTO {
    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String desc;

    /**
     * 商品备注
     */
    private String remark;

    /**
     * 价格
     */
    private double price;

    /**
     * 商品用途
     */
    private String purpose;

    /**
     * 交换地点
     */
    private String exchangeAddr;

    /**
     * 图片地址列表
     */
    private List<String> imgUrls;

    /**
     * 类型（sell/buy）
     */
    private String type;
}