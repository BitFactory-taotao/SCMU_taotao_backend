package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 管理员-举报列表项返回 DTO
 */
@Data
public class AdminReportListItemDTO {

    // 举报ID
    private Long reportId;

    // 被举报人信息
    private TargetUserVO targetUser;

    // 举报标签
    private String tag;

    // 标签中文描述
    private String tagDesc;

    // 创建时间，格式：yyyy-MM-dd HH:mm:ss
    private String createTime;

    // 审核状态：0=待审核, 1=已处理
    private Integer status;

    /**
     * 被举报人信息的值对象
     */
    @Data
    public static class TargetUserVO {
        // 学号
        private String id;
        // 姓名
        private String name;
        // 头像
        private String avatar;
    }
}

