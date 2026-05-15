package com.bit.scmu_taotao.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackDetailDTO {
	private String id;
	private String userId;
	private String userName;
	private String avatar;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime submitTime;

	private String content;
	private String replyContent;
	private String status;
	private String is_read;
}


