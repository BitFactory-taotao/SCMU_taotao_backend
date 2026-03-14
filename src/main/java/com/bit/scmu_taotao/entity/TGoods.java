package com.bit.scmu_taotao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 商品信息表
 * @TableName t_goods
 */
@TableName(value ="t_goods")
@Data
public class TGoods {
    /**
     * 商品主键
     */
    @TableId(type = IdType.AUTO)
    private Long goodsId;

    /**
     * 发布者 ID
     */
    private String userId;

    /**
     * 商品分类 ID
     */
    private Integer categoryId;

    /**
     * 商品类型（1=出售, 2=预购）
     */
    private Integer goodsType;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 描述摘要
     */
    private String goodsNote;

    /**
     * 详细描述
     */
    private String goodsDesc;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 用途/场景
     */
    private String useScene;

    /**
     * 交易地点
     */
    private String exchangePlace;

    /**
     * 状态（0=在售, 1=成交, 2=下架, 3=审核）
     */
    private Integer goodsStatus;

    /**
     * 发布时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

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
        TGoods other = (TGoods) that;
        return (this.getGoodsId() == null ? other.getGoodsId() == null : this.getGoodsId().equals(other.getGoodsId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getCategoryId() == null ? other.getCategoryId() == null : this.getCategoryId().equals(other.getCategoryId()))
            && (this.getGoodsType() == null ? other.getGoodsType() == null : this.getGoodsType().equals(other.getGoodsType()))
            && (this.getGoodsName() == null ? other.getGoodsName() == null : this.getGoodsName().equals(other.getGoodsName()))
            && (this.getGoodsNote() == null ? other.getGoodsNote() == null : this.getGoodsNote().equals(other.getGoodsNote()))
            && (this.getGoodsDesc() == null ? other.getGoodsDesc() == null : this.getGoodsDesc().equals(other.getGoodsDesc()))
            && (this.getPrice() == null ? other.getPrice() == null : this.getPrice().equals(other.getPrice()))
            && (this.getUseScene() == null ? other.getUseScene() == null : this.getUseScene().equals(other.getUseScene()))
            && (this.getExchangePlace() == null ? other.getExchangePlace() == null : this.getExchangePlace().equals(other.getExchangePlace()))
            && (this.getGoodsStatus() == null ? other.getGoodsStatus() == null : this.getGoodsStatus().equals(other.getGoodsStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getGoodsId() == null) ? 0 : getGoodsId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getCategoryId() == null) ? 0 : getCategoryId().hashCode());
        result = prime * result + ((getGoodsType() == null) ? 0 : getGoodsType().hashCode());
        result = prime * result + ((getGoodsName() == null) ? 0 : getGoodsName().hashCode());
        result = prime * result + ((getGoodsNote() == null) ? 0 : getGoodsNote().hashCode());
        result = prime * result + ((getGoodsDesc() == null) ? 0 : getGoodsDesc().hashCode());
        result = prime * result + ((getPrice() == null) ? 0 : getPrice().hashCode());
        result = prime * result + ((getUseScene() == null) ? 0 : getUseScene().hashCode());
        result = prime * result + ((getExchangePlace() == null) ? 0 : getExchangePlace().hashCode());
        result = prime * result + ((getGoodsStatus() == null) ? 0 : getGoodsStatus().hashCode());
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
        sb.append(", goodsId=").append(goodsId);
        sb.append(", userId=").append(userId);
        sb.append(", categoryId=").append(categoryId);
        sb.append(", goodsType=").append(goodsType);
        sb.append(", goodsName=").append(goodsName);
        sb.append(", goodsNote=").append(goodsNote);
        sb.append(", goodsDesc=").append(goodsDesc);
        sb.append(", price=").append(price);
        sb.append(", useScene=").append(useScene);
        sb.append(", exchangePlace=").append(exchangePlace);
        sb.append(", goodsStatus=").append(goodsStatus);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}