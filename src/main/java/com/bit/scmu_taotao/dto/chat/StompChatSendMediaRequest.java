package com.bit.scmu_taotao.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StompChatSendMediaRequest extends MessageSendMediaRequest {

    @NotNull(message = "chatId cannot be null")
    private Long chatId;

    @NotBlank(message = "receiverId cannot be blank")
    private String receiverId;
}
