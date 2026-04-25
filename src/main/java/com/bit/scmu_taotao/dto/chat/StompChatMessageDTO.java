package com.bit.scmu_taotao.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StompChatMessageDTO {
    private Long id;
    private Long chatId;
    private String senderId;
    private String receiverId;
    private String content;
    private String contentType;
    private String mediaUrl;
    private String mediaName;
    private Long mediaSize;
    private Integer mediaDuration;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;
    private Boolean read;
}

