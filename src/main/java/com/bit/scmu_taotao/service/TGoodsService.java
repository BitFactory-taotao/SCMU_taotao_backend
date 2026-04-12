package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.entity.TGoods;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * @author 35314
 * @description 针对表【t_goods(商品信息表)】的数据库操作Service
 * @createDate 2026-03-14 18:49:38
 */
public interface TGoodsService extends IService<TGoods> {
    /**
     * 根据商品ID获取商品图片URL列表
     *
     * @param goodsId 商品ID
     * @return 图片URL列表
     */
    List<String> getGoodsImageUrls(int goodsId);

    /**
     * 根据商品ID获取商品详情
     *
     * @param goodsId 商品ID
     * @return 商品详情
     */
    TGoods getGoodsById(int goodsId);

    /**
     * 首页Tab商品列表
     * tab优先，category为兼容参数
     */
    Result getHomeGoodsList(String tab, String category, Integer page, Integer size, String currentUserId);

    Result searchHomeGoods(@NotBlank(message = "keyword不能为空") String keyword, @Min(value = 1, message = "page必须大于等于1") Integer page, @Min(value = 1, message = "size必须大于等于1") @Max(value = 50, message = "size不能超过50") Integer size);
}
