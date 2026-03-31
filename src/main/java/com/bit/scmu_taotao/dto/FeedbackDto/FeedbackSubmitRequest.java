package com.bit.scmu_taotao.dto.FeedbackDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class FeedbackSubmitRequest {
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 1000, message = "反馈内容不能超过1000字")
    private String content;
}