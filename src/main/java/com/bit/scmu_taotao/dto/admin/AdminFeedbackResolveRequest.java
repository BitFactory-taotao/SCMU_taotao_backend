package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminFeedbackResolveRequest {
    @NotBlank(message = "replyContent不能为空")
    @Size(max = 2500, message = "回复内容不能超过2500字")
    private String replyContent;
}

