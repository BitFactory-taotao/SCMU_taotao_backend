package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天会话表
 * @TableName chat_session
 */
@TableName(value ="chat_session")
@Data
public class ChatSession {
    /**
     * 会话主键
     */
    @TableId(type = IdType.AUTO)
    private Long chatId;

    /**
     * 用户A
     */
    private String user1Id;

    /**
     * 用户B
     */
    private String user2Id;

    /**
     * 最后信息
     */
    private String lastMsg;

    /**
     * 最后聊天时间
     */
    private Date lastTime;

    /**
     * 状态（1-正常, 2-拉黑, 3-删除）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdAt;

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
        ChatSession other = (ChatSession) that;
        return (this.getChatId() == null ? other.getChatId() == null : this.getChatId().equals(other.getChatId()))
            && (this.getUser1Id() == null ? other.getUser1Id() == null : this.getUser1Id().equals(other.getUser1Id()))
            && (this.getUser2Id() == null ? other.getUser2Id() == null : this.getUser2Id().equals(other.getUser2Id()))
            && (this.getLastMsg() == null ? other.getLastMsg() == null : this.getLastMsg().equals(other.getLastMsg()))
            && (this.getLastTime() == null ? other.getLastTime() == null : this.getLastTime().equals(other.getLastTime()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getChatId() == null) ? 0 : getChatId().hashCode());
        result = prime * result + ((getUser1Id() == null) ? 0 : getUser1Id().hashCode());
        result = prime * result + ((getUser2Id() == null) ? 0 : getUser2Id().hashCode());
        result = prime * result + ((getLastMsg() == null) ? 0 : getLastMsg().hashCode());
        result = prime * result + ((getLastTime() == null) ? 0 : getLastTime().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", chatId=").append(chatId);
        sb.append(", user1Id=").append(user1Id);
        sb.append(", user2Id=").append(user2Id);
        sb.append(", lastMsg=").append(lastMsg);
        sb.append(", lastTime=").append(lastTime);
        sb.append(", status=").append(status);
        sb.append(", createdAt=").append(createdAt);
        sb.append("]");
        return sb.toString();
    }
}