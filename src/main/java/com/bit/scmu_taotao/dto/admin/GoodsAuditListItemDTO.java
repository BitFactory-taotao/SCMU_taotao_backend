package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class GoodsAuditListItemDTO {
    private Long id;
    private String name;
    private String remark;
    private BigDecimal price;
    private String imgUrl;
    private String publishTime;
    private String publisherName;
    private String publisherId;
}
