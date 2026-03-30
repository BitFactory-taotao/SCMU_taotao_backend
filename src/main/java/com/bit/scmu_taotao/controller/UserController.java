package com.bit.scmu_taotao.Controller;

import com.bit.scmu_taotao.dto.GoodsEditRequest;
import com.bit.scmu_taotao.service.TUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.bit.scmu_taotao.util.common.Result;

import lombok.extern.slf4j.Slf4j;


@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private TUserService tUserService;

/**
 * 获取用户信息接口
 * 通过GET请求方式获取当前登录用户的详细信息
 *
 * @return 返回一个Result对象，包含用户信息数据
 */
    @GetMapping("/info")
    public Result getUserInfo() {
        return tUserService.getUserInfo();
    }
/**
 * 获取收藏列表的接口方法
 * @param page 页码数，默认值为1
 * @param size 每页大小，默认值为10
 * @return 返回Result对象，包含收藏列表数据
 */
    @GetMapping("/favorites")    // HTTP GET请求映射到"/favorites"路径
    public Result getFavorites(@RequestParam(defaultValue = "1") Integer page,  // 页码参数，默认为1
                               @RequestParam(defaultValue = "10") Integer size) { // 每页大小参数，默认为10
        return tUserService.getFavorites(page, size);  // 调用服务层方法获取收藏列表并返回
    }
/**
 * 获取销售商品列表的接口方法
 * @param page 页码，默认值为1
 * @param size 每页大小，默认值为10
 * @param goodsStatus 商品状态
 * @return 返回Result对象，包含销售商品列表数据
 */
    @GetMapping("/goods/sell")
    public Result getSellGoods(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size, @RequestParam("goodsStatus") String goodsStatus) {
    // 调用用户服务层的getSellGoods方法，传入页码、每页大小和商品状态参数
        return tUserService.getSellGoods(page, size,goodsStatus);
    }

    /**
     * 获取我买到的商品列表
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 买到的商品列表
     */
    @GetMapping("/goods/bought")
    public Result getBoughtGoods(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        return tUserService.getBoughtGoods(page, size);
    }

    /**
     * 获取我想要的商品列表（预购）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @param status 状态 (online/offline)
     * @return 预购商品列表
     */
    @GetMapping("/goods/buy")
    public Result getBuyGoods(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              @RequestParam("status") String status) {
        return tUserService.getBuyGoods(page, size, status);
    }
    /**
     * 编辑我的商品/预购需求
     * @param goodsId 商品ID
     * @param request 编辑请求
     * @return 编辑结果
     */
    @PutMapping("/goods/{goodsId}")
    public Result editGoods(@PathVariable Long goodsId,
                            @Valid @RequestBody GoodsEditRequest request) {
        return tUserService.editGoods(goodsId, request);
    }
    /**
     * 下架我的商品/预购需求
     * @param goodsId 商品ID
     * @return 下架结果
     */
    @PutMapping("/goods/{goodsId}/offline")
    public Result offlineGoods(@PathVariable Long goodsId) {
        return tUserService.offlineGoods(goodsId);
    }
    /**
     * 删除我的商品/预购需求
     * @param goodsId 商品ID
     * @return 删除结果
     */
    @DeleteMapping("/goods/{goodsId}")
    public Result deleteGoods(@PathVariable Long goodsId) {
        return tUserService.deleteGoods(goodsId);
    }
}
