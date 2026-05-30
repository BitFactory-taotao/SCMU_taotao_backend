package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

@Data
public class SolvedItemDetailResponse {
    private String type;
    private String handleStatus;
    private String handleTime;
    private SolvedGoodsDetailDTO goodsDetail;
    private SolvedFeedbackDetailDTO feedbackDetail;
    private SolvedUserDetailDTO userDetail;
}
