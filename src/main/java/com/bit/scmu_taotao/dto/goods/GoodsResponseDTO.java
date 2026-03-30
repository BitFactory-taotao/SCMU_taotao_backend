package com.bit.scmu_taotao.dto.goods;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsResponseDTO {
    private Long id;
    private String name;
    private String desc;
    private BigDecimal price;
    private String purpose;
    private String exchangeAddr;
    private List<String> imgUrls;
    private String publishTime; // 格式化后的时间字符串
    private PublisherDTO publisher;
    private String type; // sell/buy
}
