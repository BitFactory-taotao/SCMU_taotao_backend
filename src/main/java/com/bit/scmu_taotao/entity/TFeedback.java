package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 用户反馈建议表
 * @TableName t_feedback
 */
@TableName(value ="t_feedback")
@Data
public class TFeedback {
    /**
     * 反馈主键
     */
    @TableId(type = IdType.AUTO)
    private Long feedbackId;

    /**
     * 反馈者
     */
    private String userId;

    /**
     * 内容
     */
    private String feedbackContent;

    /**
     * 状态（0-待, 1-中, 2-复, 3-闭）
     * 0-pending, 1-processed
     */
    private Integer feedbackStatus;

    // 管理员是否已读: 0未读, 1已读
    private Integer isRead;
    /**
     * 回复内容
     */
    private String replyContent;

    /**
     * 回复时间
     */
    private LocalDateTime replyTime;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 状态更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 软删除
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
        TFeedback other = (TFeedback) that;
        return (this.getFeedbackId() == null ? other.getFeedbackId() == null : this.getFeedbackId().equals(other.getFeedbackId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getFeedbackContent() == null ? other.getFeedbackContent() == null : this.getFeedbackContent().equals(other.getFeedbackContent()))
            && (this.getFeedbackStatus() == null ? other.getFeedbackStatus() == null : this.getFeedbackStatus().equals(other.getFeedbackStatus()))
            && (this.getReplyContent() == null ? other.getReplyContent() == null : this.getReplyContent().equals(other.getReplyContent()))
            && (this.getReplyTime() == null ? other.getReplyTime() == null : this.getReplyTime().equals(other.getReplyTime()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFeedbackId() == null) ? 0 : getFeedbackId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getFeedbackContent() == null) ? 0 : getFeedbackContent().hashCode());
        result = prime * result + ((getFeedbackStatus() == null) ? 0 : getFeedbackStatus().hashCode());
        result = prime * result + ((getReplyContent() == null) ? 0 : getReplyContent().hashCode());
        result = prime * result + ((getReplyTime() == null) ? 0 : getReplyTime().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", feedbackId=").append(feedbackId);
        sb.append(", userId=").append(userId);
        sb.append(", feedbackContent=").append(feedbackContent);
        sb.append(", feedbackStatus=").append(feedbackStatus);
        sb.append(", replyContent=").append(replyContent);
        sb.append(", replyTime=").append(replyTime);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}