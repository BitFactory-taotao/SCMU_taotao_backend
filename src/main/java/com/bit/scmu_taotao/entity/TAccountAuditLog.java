package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName(value = "t_account_audit_log")
@Data
public class TAccountAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    // ban、clear
    private String action;
    private Integer previousStatus;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer isDelete;
}
