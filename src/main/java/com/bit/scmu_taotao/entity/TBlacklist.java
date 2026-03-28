package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 黑名单表
 * @TableName t_blacklist
 */
@TableName(value ="t_blacklist")
@Data
public class TBlacklist {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long blackId;

    /**
     * 拉黑者
     */
    private String userId;

    /**
     * 被拉黑者
     */
    private String blackUserId;

    /**
     * 时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 解除标记
     */
    private Integer isDelete;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        TBlacklist other = (TBlacklist) that;
        return (this.getBlackId() == null ? other.getBlackId() == null : this.getBlackId().equals(other.getBlackId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getBlackUserId() == null ? other.getBlackUserId() == null : this.getBlackUserId().equals(other.getBlackUserId()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getBlackId() == null) ? 0 : getBlackId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getBlackUserId() == null) ? 0 : getBlackUserId().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", blackId=").append(blackId);
        sb.append(", userId=").append(userId);
        sb.append(", blackUserId=").append(blackUserId);
        sb.append(", createTime=").append(createTime);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}