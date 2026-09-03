package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.dto.goods.GoodsRequestDTO;
import com.bit.scmu_taotao.dto.goods.GoodsResponseDTO;
import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import com.bit.scmu_taotao.dto.goods.SearchRequestDTO;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.service.impl.TFavoriteServiceImpl;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private TGoodsService tGoodsService;
    @Autowired
    private TUserService tUserService;
    @Autowired
    private TFavoriteServiceImpl tFavoriteService;
    @Autowired
    private ChatSessionService chatSessionService;
    @Autowired
    private BrowseRecorder browseRecorder;
    /**
     * 首页Tab商品列表
     * 支持tab参数，category作为兼容参数
     */
    @GetMapping
    public Result getHomeGoodsList(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String currentUserId = UserContext.getUserId();
        return tGoodsService.getHomeGoodsList(tab, category, page, size, currentUserId);
    }

    /**
     * 首页模糊搜索（匹配商品名称、描述、备注）
     */
    @GetMapping("/search")
    public Result searchHomeGoods(@Valid SearchRequestDTO query, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }

        log.info("首页搜索请求：keyword={}, page={}, size={}", query.getKeyword(), query.getPage(), query.getSize());
        return tGoodsService.searchHomeGoods(query.getKeyword(), query.getPage(), query.getSize());
    }

    @PostMapping
    public Result publishGoods(@RequestBody GoodsRequestDTO request) {
        return tGoodsService.publishGoods(request);
    }

    // 保存草稿接口
    @PostMapping("/draft")
    public Result saveDraft(@RequestBody GoodsRequestDTO request) {
        return tGoodsService.saveDraft(request);
    }

    // 获取草稿列表接口
    @GetMapping("/draft")
    public Result getDraftList() {
        return tGoodsService.getDraftList();
    }

    // 获取草稿详情接口
    @GetMapping("/draft/{draftId}")
    public Result getDraftDetail(@PathVariable("draftId") String draftId) {
        return tGoodsService.getDraftDetail(draftId);
    }

    // 删除草稿接口
    @DeleteMapping("/draft/{draftId}")
    public Result deleteDraft(@PathVariable("draftId") String draftId) {
        return tGoodsService.deleteDraft(draftId);
    }

    @GetMapping("/{goodsId}")
    public Result getGoods(@PathVariable("goodsId") @Min(1) int goodsId) {
        // 1. 获取商品基本信息
        TGoods goods = tGoodsService.getGoodsById(goodsId);

        // 异步埋点：记录浏览 + 更新点击量（防刷机制内置）
        String currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            browseRecorder.recordBrowse(currentUserId, (long) goodsId);
        }

        // 2. 构建响应DTO
        GoodsResponseDTO data = new GoodsResponseDTO();
        data.setId(goods.getGoodsId());
        data.setName(goods.getGoodsName());
        data.setDesc(goods.getGoodsDesc());
        data.setPrice(goods.getPrice());
        data.setPurpose(goods.getUseScene());
        data.setExchangeAddr(goods.getExchangePlace());

        // 3. 格式化发布时间
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        data.setPublishTime(goods.getCreateTime().format(dtf));

        // 4. 设置商品类型
        data.setType(goods.getGoodsType() == 1 ? "sell" : "buy");

        // 5. 获取并设置图片URLs（需要在TGoodsService中添加相应方法）
        List<String> imgUrls = tGoodsService.getGoodsImageUrls(goodsId);
        data.setImgUrls(imgUrls);

        // 6. 获取并设置发布者信息（需要在TUserService中添加相应方法）
        PublisherDTO publisher = tUserService.getPublisherInfo(goods.getUserId());
        data.setPublisher(publisher);

        return Result.ok("请求成功", data);
    }

    @PostMapping("/{goodsId}/favorite")
    public Result favorite(@PathVariable("goodsId") @Min(1) int goodsId) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        long gid = (long) goodsId;
        // 2. 查询是否已有收藏记录（包含已删除）
        LambdaQueryWrapper<TFavorite> query = new LambdaQueryWrapper<>();
        query.eq(TFavorite::getUserId, userId)
                .eq(TFavorite::getGoodsId, gid)
                .last("LIMIT 1");
        TFavorite existed = tFavoriteService.getOne(query);

        // 3. 去重处理
        if (existed != null) {
            // 已收藏，直接返回
            if (Integer.valueOf(0).equals(existed.getIsDelete())) {
                return Result.ok("已收藏，无需重复操作", null);
            }
            // 历史记录被取消过，恢复收藏
            existed.setIsDelete(0);
            tFavoriteService.updateById(existed);
            return Result.ok("收藏成功", null);
        }
        // 4. 创建收藏记录
        TFavorite favorite = new TFavorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(gid);
        favorite.setIsDelete(0);
        // 5. 保存到数据库
        tFavoriteService.save(favorite);
        // 6. 返回成功响应
        return Result.ok("收藏成功", null);
    }

    @DeleteMapping("/{goodsId}/favorite")
    public Result unfavorite(@PathVariable("goodsId") @Min(1) int goodsId) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        long gid = (long) goodsId;

        // 2. 查询当前有效收藏记录
        LambdaQueryWrapper<TFavorite> query = new LambdaQueryWrapper<>();
        query.eq(TFavorite::getUserId, userId)
                .eq(TFavorite::getGoodsId, gid)
                .eq(TFavorite::getIsDelete, 0)
                .last("LIMIT 1");
        TFavorite existed = tFavoriteService.getOne(query);

        // 3. 幂等处理：没找到也返回取消成功
        if (existed == null) {
            return Result.ok("取消收藏成功", null);
        }

        // 4. 软删除收藏记录
        existed.setIsDelete(1);
        tFavoriteService.updateById(existed);

        // 5. 返回成功响应
        return Result.ok("取消收藏成功", null);
    }

    // todo 后续可删掉，保留contactseller即可
    @GetMapping("/{goodsId}/trade")
    public Result contactSeller(@PathVariable("goodsId") @Min(1) int goodsId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatSessionService.contactSeller((long) goodsId, userId);
    }


}
