package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 评价附图表
 * @TableName t_evaluate_image
 */
@TableName(value ="t_evaluate_image")
@Data
public class TEvaluateImage {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long evalImgId;

    /**
     * 关联评价
     */
    private Long evalId;

    /**
     * 图 URL
     */
    private String imgUrl;

    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

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
        TEvaluateImage other = (TEvaluateImage) that;
        return (this.getEvalImgId() == null ? other.getEvalImgId() == null : this.getEvalImgId().equals(other.getEvalImgId()))
            && (this.getEvalId() == null ? other.getEvalId() == null : this.getEvalId().equals(other.getEvalId()))
            && (this.getImgUrl() == null ? other.getImgUrl() == null : this.getImgUrl().equals(other.getImgUrl()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEvalImgId() == null) ? 0 : getEvalImgId().hashCode());
        result = prime * result + ((getEvalId() == null) ? 0 : getEvalId().hashCode());
        result = prime * result + ((getImgUrl() == null) ? 0 : getImgUrl().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", evalImgId=").append(evalImgId);
        sb.append(", evalId=").append(evalId);
        sb.append(", imgUrl=").append(imgUrl);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}