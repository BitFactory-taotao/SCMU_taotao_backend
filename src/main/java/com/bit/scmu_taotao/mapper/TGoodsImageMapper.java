package com.bit.scmu_taotao.mapper;

import com.bit.scmu_taotao.entity.TGoodsImage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 35314
* @description 针对表【t_goods_image(商品图片关联表)】的数据库操作Mapper
* @createDate 2026-03-14 18:49:38
* @Entity com.bit.scmu_taotao.entity.TGoodsImage
*/
public interface TGoodsImageMapper extends BaseMapper<TGoodsImage> {

    /**
     * 根据商品ID查询图片列表
     * @param goodsId 商品ID
     * @return 图片列表
     */
    List<TGoodsImage> selectByGoodsId(int goodsId);
}




