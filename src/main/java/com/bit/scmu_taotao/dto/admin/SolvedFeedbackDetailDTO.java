package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

@Data
public class SolvedFeedbackDetailDTO {
    private Long feedbackId;
    private String userId;
    private String userName;
    private String avatar;
    private String submitTime;
    private String content;
    private String replyContent;
}
