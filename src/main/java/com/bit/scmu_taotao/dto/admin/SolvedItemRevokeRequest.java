package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SolvedItemRevokeRequest {
    @NotBlank(message = "type不能为空")
    private String type;
    @NotBlank(message = "id不能为空")
    private String id;
}
