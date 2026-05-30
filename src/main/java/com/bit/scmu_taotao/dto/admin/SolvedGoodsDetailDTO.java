package com.bit.scmu_taotao.dto.admin;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SolvedGoodsDetailDTO {
    private Long goodsId;
    private String name;
    private String desc;
    private String remark;
    private BigDecimal price;
    private String purpose;
    private String exchangeAddr;
    List<String> imgUrls;
    private String publishTime;
    private PublisherDTO publisher;
    private String goodsType;
    private String rejectReason;

    @Data
    public static class PublisherDTO {
        private String userId;
        private String userName;
        private String avatar;
        private Integer creditScore;
        private java.math.BigDecimal creditStar;
    }
}
