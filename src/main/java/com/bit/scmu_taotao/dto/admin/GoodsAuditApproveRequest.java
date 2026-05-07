package com.bit.scmu_taotao.dto.admin;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
/**
 * 商品巡检通过请求
 */
@Data
public class GoodsAuditApproveRequest {
    /**
     * 商品ID列表
     */
    @NotEmpty(message = "goodsIds不能为空")
    private List<Long> goodsIds;
}
