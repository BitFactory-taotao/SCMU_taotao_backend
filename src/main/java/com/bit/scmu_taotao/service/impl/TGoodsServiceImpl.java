package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TGoodsImage;
import com.bit.scmu_taotao.mapper.TGoodsImageMapper;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.mapper.TGoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 35314
* @description 针对表【t_goods(商品信息表)】的数据库操作Service实现
* @createDate 2026-03-14 18:49:38
*/
@Service
public class TGoodsServiceImpl extends ServiceImpl<TGoodsMapper, TGoods>
    implements TGoodsService{
    @Autowired
    private TGoodsImageMapper tGoodsImageMapper;

    @Override
    public List<String> getGoodsImageUrls(int goodsId) {
        // 根据商品ID查询图片列表
        List<TGoodsImage> images = tGoodsImageMapper.selectByGoodsId(goodsId);
        // 提取图片URL并返回
        return images.stream()
                .map(TGoodsImage::getImageUrl)
                .collect(Collectors.toList());
    }

    @Override
    public TGoods getGoodsById(int goodsId) {
        // 调用父类的getById方法获取商品信息
        return this.getById(goodsId);
    }
}




