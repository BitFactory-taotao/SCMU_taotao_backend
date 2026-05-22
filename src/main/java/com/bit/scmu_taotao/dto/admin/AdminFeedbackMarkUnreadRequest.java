package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AdminFeedbackMarkUnreadRequest {
    @NotEmpty(message = "feedbackIds不能为空")
    @Size(max = 20, message = "一次最多只能标记20条反馈为未读")
    private List<Long> feedbackIds;
}

