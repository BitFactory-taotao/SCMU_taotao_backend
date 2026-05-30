package com.bit.scmu_taotao.dto.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SolvedUserDetailDTO {
    private String userId;
    private String userName;
    private String avatar;
    private Integer creditScore;
    private BigDecimal creditStar;
    private String reasonCategory;
    private String violationReason;
}
