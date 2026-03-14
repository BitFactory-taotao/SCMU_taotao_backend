package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 交易评价表
 * @TableName t_evaluate
 */
@TableName(value ="t_evaluate")
@Data
public class TEvaluate {
    /**
     * 评价主键
     */
    @TableId(type = IdType.AUTO)
    private Long evalId;

    /**
     * 交易 ID
     */
    private Long tradeId;

    /**
     * 商品 ID
     */
    private Long goodsId;

    /**
     * 评价者
     */
    private String buyerId;

    /**
     * 被评价者
     */
    private String sellerId;

    /**
     * 相符分
     */
    private Integer descScore;

    /**
     * 沟通分
     */
    private Integer commScore;

    /**
     * 总体分
     */
    private Integer totalScore;

    /**
     * 文字内容
     */
    private String evalContent;

    /**
     * 是否匿名
     */
    private Integer isAnonymous;

    /**
     * 评价时间
     */
    private Date createTime;

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
        TEvaluate other = (TEvaluate) that;
        return (this.getEvalId() == null ? other.getEvalId() == null : this.getEvalId().equals(other.getEvalId()))
            && (this.getTradeId() == null ? other.getTradeId() == null : this.getTradeId().equals(other.getTradeId()))
            && (this.getGoodsId() == null ? other.getGoodsId() == null : this.getGoodsId().equals(other.getGoodsId()))
            && (this.getBuyerId() == null ? other.getBuyerId() == null : this.getBuyerId().equals(other.getBuyerId()))
            && (this.getSellerId() == null ? other.getSellerId() == null : this.getSellerId().equals(other.getSellerId()))
            && (this.getDescScore() == null ? other.getDescScore() == null : this.getDescScore().equals(other.getDescScore()))
            && (this.getCommScore() == null ? other.getCommScore() == null : this.getCommScore().equals(other.getCommScore()))
            && (this.getTotalScore() == null ? other.getTotalScore() == null : this.getTotalScore().equals(other.getTotalScore()))
            && (this.getEvalContent() == null ? other.getEvalContent() == null : this.getEvalContent().equals(other.getEvalContent()))
            && (this.getIsAnonymous() == null ? other.getIsAnonymous() == null : this.getIsAnonymous().equals(other.getIsAnonymous()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEvalId() == null) ? 0 : getEvalId().hashCode());
        result = prime * result + ((getTradeId() == null) ? 0 : getTradeId().hashCode());
        result = prime * result + ((getGoodsId() == null) ? 0 : getGoodsId().hashCode());
        result = prime * result + ((getBuyerId() == null) ? 0 : getBuyerId().hashCode());
        result = prime * result + ((getSellerId() == null) ? 0 : getSellerId().hashCode());
        result = prime * result + ((getDescScore() == null) ? 0 : getDescScore().hashCode());
        result = prime * result + ((getCommScore() == null) ? 0 : getCommScore().hashCode());
        result = prime * result + ((getTotalScore() == null) ? 0 : getTotalScore().hashCode());
        result = prime * result + ((getEvalContent() == null) ? 0 : getEvalContent().hashCode());
        result = prime * result + ((getIsAnonymous() == null) ? 0 : getIsAnonymous().hashCode());
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
        sb.append(", evalId=").append(evalId);
        sb.append(", tradeId=").append(tradeId);
        sb.append(", goodsId=").append(goodsId);
        sb.append(", buyerId=").append(buyerId);
        sb.append(", sellerId=").append(sellerId);
        sb.append(", descScore=").append(descScore);
        sb.append(", commScore=").append(commScore);
        sb.append(", totalScore=").append(totalScore);
        sb.append(", evalContent=").append(evalContent);
        sb.append(", isAnonymous=").append(isAnonymous);
        sb.append(", createTime=").append(createTime);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}