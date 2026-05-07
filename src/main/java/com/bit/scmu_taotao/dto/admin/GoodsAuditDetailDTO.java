package com.bit.scmu_taotao.dto.admin;

import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品巡检详情
 */
@Data
public class GoodsAuditDetailDTO {

    private Long id;
    private String name;
    private String desc;
    private BigDecimal price;
    private String purpose;
    private String exchangeAddr;
    private List<String> imgUrls;
    private String publishTime;
    private PublisherDTO publisher;
    private String type;
}
