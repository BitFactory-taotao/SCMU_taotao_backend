package com.bit.scmu_taotao.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StompChatSendMediaRequest {

    @NotNull(message = "chatId cannot be null")
    private Long chatId;

    @NotBlank(message = "receiverId cannot be blank")
    private String receiverId;

    @NotBlank(message = "contentType cannot be blank")
    @Pattern(regexp = "TEXT|IMAGE|AUDIO", message = "contentType must be TEXT/IMAGE/AUDIO")
    private String contentType;

    @Size(max = 1000, message = "content length cannot exceed 1000")
    private String content;

    @NotBlank(message = "mediaUrl cannot be blank")
    @Size(max = 512, message = "mediaUrl length cannot exceed 512")
    private String mediaUrl;

    @Size(max = 255, message = "mediaName length cannot exceed 255")
    private String mediaName;

    private Long mediaSize;

    private Integer mediaDuration;
}

