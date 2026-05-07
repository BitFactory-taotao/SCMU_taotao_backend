package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
/**
 * 商品巡检驳回请求
 */
@Data
public class GoodsAuditRejectRequest {
    /**
     * 商品ID列表
     */
    @NotEmpty(message = "goodsIds不能为空")
    private List<Long> goodsIds;
    /**
     * 驳回原因
     */
    @NotBlank(message = "reason不能为空")
    private String reason;
}
