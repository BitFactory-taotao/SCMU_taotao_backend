package com.bit.scmu_taotao.dto.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SolvedGoodsItemDTO {
    private Long goodsId;
    private String name;
    private String remark;
    private BigDecimal price;
    private String imgUrl;
    private String publishTime;
    private String handleStatus;
    private String handleTime;
}
