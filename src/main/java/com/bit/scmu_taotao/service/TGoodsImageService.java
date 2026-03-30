package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.entity.TGoodsImage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 35314
* @description 针对表【t_goods_image(商品图片关联表)】的数据库操作Service
* @createDate 2026-03-14 18:49:38
*/
public interface TGoodsImageService extends IService<TGoodsImage> {
    List<String> getGoodsImageUrls(Long goodsId);
}
