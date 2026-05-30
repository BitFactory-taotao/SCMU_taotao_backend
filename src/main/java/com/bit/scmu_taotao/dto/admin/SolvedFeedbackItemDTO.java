package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

@Data
public class SolvedFeedbackItemDTO {
    private Long feedbackId;
    private String userId;
    private String userName;
    private String avatar;
    private String replyContent;
    private String submitTime;
    private String replyTime;
}
