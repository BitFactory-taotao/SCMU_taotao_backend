package com.bit.scmu_taotao.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StompChatSendRequest {

    @NotNull(message = "chatId cannot be null")
    private Long chatId;

    @NotBlank(message = "receiverId cannot be blank")
    private String receiverId;

    @NotBlank(message = "content cannot be blank")
    @Size(max = 1000, message = "content length cannot exceed 1000")
    private String content;
}

