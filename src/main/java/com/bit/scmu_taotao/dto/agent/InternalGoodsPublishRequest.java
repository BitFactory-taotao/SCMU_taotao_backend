package com.bit.scmu_taotao.dto.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal API — 发布商品请求
 * 与公开 API 的区别：显式传入 userId（Agent 已验证用户身份）
 */
@Data
public class InternalGoodsPublishRequest {
    /**
     * 用户 ID（学号，由 Agent 透传）
     */
    private String userId;

    private String name;
    private String desc;
    private String remark;
    private BigDecimal price;
    private String purpose;
    private String exchangeAddr;
    private List<String> imgUrls;

    /**
     * 商品类型：sell / buy
     */
    private String type;

    /**
     * 分类名称（如"学习用品"），非 ID
     */
    private String categoryName;

    /**
     * 草稿 ID（可选，发布后删除对应草稿）
     */
    private String draftId;
}
