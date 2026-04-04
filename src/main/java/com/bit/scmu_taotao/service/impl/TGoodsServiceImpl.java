package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.scmu_taotao.dto.recommend.RecommendGoodsDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendListResponseDTO;
import com.bit.scmu_taotao.entity.TGoods;
import com.bit.scmu_taotao.entity.TGoodsCategory;
import com.bit.scmu_taotao.entity.TGoodsImage;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.TGoodsImageMapper;
import com.bit.scmu_taotao.service.RecommendationService;
import com.bit.scmu_taotao.service.TGoodsCategoryService;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.mapper.TGoodsMapper;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private TUserService tUserService;

    private static final Set<String> VALID_TABS = Set.of(
            "recommend", "dormitory", "entertainment", "study", "pre-order"
    );

    private static final Map<String, String> TAB_TO_CATEGORY_NAME = Map.of(
            "dormitory", "宿舍用品",
            "entertainment", "娱乐用品",
            "study", "学习用品"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    @Override
    public Result getHomeGoodsList(String tab, String category, Integer page, Integer size, String currentUserId) {
        String effectiveTab = normalizeTab(tab, category);
        if (!VALID_TABS.contains(effectiveTab)) {
            return Result.fail(400, "tab参数非法");
        }

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        if ("recommend".equals(effectiveTab)) {
            return buildRecommendResult(currentUserId, safePage, safeSize);
        }

        return buildNormalTabResult(effectiveTab, safePage, safeSize);
    }

    private String normalizeTab(String tab, String category) {
        if (tab != null && !tab.isBlank()) {
            return tab.trim();
        }
        if (category != null && !category.isBlank()) {
            return category.trim();
        }
        return "recommend";
    }

    private Result buildRecommendResult(String currentUserId, int page, int size) {
        String userIdForRecommend = currentUserId == null ? "anonymous" : currentUserId;
        RecommendListResponseDTO recommendResult = recommendationService.getRecommendations(userIdForRecommend, page, size);

        List<Map<String, Object>> list = new ArrayList<>();
        if (recommendResult.getList() != null) {
            for (RecommendGoodsDTO item : recommendResult.getList()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", item.getGoodsId());
                row.put("name", item.getGoodsName());
                row.put("remark", "");
                row.put("price", item.getPrice());
                row.put("imgUrl", item.getImageUrl() == null ? "" : item.getImageUrl());
                row.put("publishTime", item.getCreateTime());
                row.put("publisherName", item.getPublisherInfo() == null ? "未知" : item.getPublisherInfo().getUserName());
                row.put("publisherId", item.getPublisherInfo() == null ? "" : item.getPublisherInfo().getUserId());
                list.add(row);
            }
        }

        long total = recommendResult.getTotal() == null ? list.size() : recommendResult.getTotal();
        long pages = total == 0 ? 0 : (total + size - 1) / size;

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("pages", pages);
        data.put("list", list);

        return Result.ok("请求成功", data);
    }

    private Result buildNormalTabResult(String tab, int page, int size) {
        LambdaQueryWrapper<TGoods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TGoods::getIsDelete, 0)
                .eq(TGoods::getGoodsStatus, 0)
                .orderByDesc(TGoods::getCreateTime);

        if ("pre-order".equals(tab)) {
            queryWrapper.eq(TGoods::getGoodsType, 2);
        } else {
            queryWrapper.eq(TGoods::getGoodsType, 1);
            Integer categoryId = resolveCategoryIdByTab(tab);
            if (categoryId == null) {
                return Result.fail(400, "tab参数非法");
            }
            queryWrapper.eq(TGoods::getCategoryId, categoryId);
        }

        Page<TGoods> goodsPage = this.page(new Page<>(page, size), queryWrapper);

        List<Map<String, Object>> list = goodsPage.getRecords().stream().map(goods -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", goods.getGoodsId());
            row.put("name", goods.getGoodsName());
            row.put("remark", goods.getGoodsNote() == null ? "" : goods.getGoodsNote());
            row.put("price", goods.getPrice());
            row.put("imgUrl", getMainImage(goods.getGoodsId()));
            row.put("publishTime", goods.getCreateTime() == null ? "" : goods.getCreateTime().format(DATE_TIME_FORMATTER));

            TUser publisher = tUserService.getById(goods.getUserId());
            row.put("publisherName", publisher == null ? "未知" : publisher.getUserName());
            row.put("publisherId", goods.getUserId());
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", goodsPage.getTotal());
        data.put("pages", goodsPage.getPages());
        data.put("list", list);

        return Result.ok("请求成功", data);
    }

    private Integer resolveCategoryIdByTab(String tab) {
        String categoryName = TAB_TO_CATEGORY_NAME.get(tab);
        if (categoryName == null) {
            return null;
        }
        QueryWrapper<TGoodsCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_name", categoryName).last("LIMIT 1");
        TGoodsCategory category = tGoodsCategoryService.getOne(wrapper);
        return category == null ? null : category.getCategoryId();
    }

    private String getMainImage(Long goodsId) {
        List<TGoodsImage> images = tGoodsImageMapper.selectByGoodsId(goodsId.intValue());
        if (images == null || images.isEmpty() || images.get(0).getImageUrl() == null) {
            return "";
        }
        return images.get(0).getImageUrl();
    }
}




