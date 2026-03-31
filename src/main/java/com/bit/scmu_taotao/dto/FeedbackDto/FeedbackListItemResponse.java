package com.bit.scmu_taotao.dto.FeedbackDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeedbackListItemResponse {
    private Long id;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    private String replyContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

    private String status; // "pending" 或 "processed"
}

