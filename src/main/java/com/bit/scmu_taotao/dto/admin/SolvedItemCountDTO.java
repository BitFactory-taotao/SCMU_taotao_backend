package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

@Data
public class SolvedItemCountDTO {
    private long goodsCount;
    private long feedbackCount;
    private long userCount;
    private long totalCount;
}
