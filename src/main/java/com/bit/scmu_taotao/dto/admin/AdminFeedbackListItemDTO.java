package com.bit.scmu_taotao.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackListItemDTO {
    private String id;
    private String userId;
    private String userName;
    private String avatar;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    private String status; // pending / processed

    private String is_read; // read / unread
}

