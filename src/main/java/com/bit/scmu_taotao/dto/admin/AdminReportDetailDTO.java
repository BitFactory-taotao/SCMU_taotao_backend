package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 管理员-举报详情返回 DTO
 */
@Data
public class AdminReportDetailDTO {

    // 举报ID
    private Long reportId;

    // 被举报人信息
    private AdminReportListItemDTO.TargetUserVO targetUser;

    // 举报标签
    private String tag;

    // 标签中文描述
    private String tagDesc;

    // 举报详情描述
    private String content;

    // 证据图片地址列表
    private List<String> imgUrls;

    // 创建时间，格式：yyyy-MM-dd HH:mm:ss
    private String createTime;
}

