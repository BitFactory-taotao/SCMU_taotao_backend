package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminFeedbackResolveRequest {
    @NotBlank(message = "replyContent不能为空")
    private String replyContent;
}

