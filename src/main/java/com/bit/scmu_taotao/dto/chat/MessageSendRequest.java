package com.bit.scmu_taotao.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageSendRequest {

    @NotBlank(message = "content cannot be blank")
    @Size(max = 1000, message = "content length cannot exceed 1000")
    private String content;
}

