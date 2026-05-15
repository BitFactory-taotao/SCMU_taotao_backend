package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminFeedbackMarkUnreadRequest {
    @NotEmpty(message = "feedbackIds不能为空")
    private List<Long> feedbackIds;
}

