package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.GoodsEditRequest;
import com.bit.scmu_taotao.entity.TUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;

/**
* @author 35314
* @description 针对表【t_user(用户基础信息表)】的数据库操作 Service
* @createDate 2026-03-12 18:35:11
*/
public interface TUserService extends IService<TUser> {

    /**
     * 用户登录
     * @param userId 用户 ID（学号/工号）
     * @param password 密码
     * @return 登录结果，包含 token 和用户信息
     */
    Result login(String userId, String password);

    /**
     * 用户登出
     * @param token Token
     * @return 登出结果
     */
    Result logout(String token);

    /**
     * 根据 Token 获取用户信息
     * @param token Token
     * @return 用户信息
     */
    Result getUserInfoByToken(String token);

    Result getUserInfo();

    Result getFavorites(Integer page, Integer size);

    Result getSellGoods(Integer page, Integer size,String goodsStatus);

    /**
     * 获取我买到的商品列表
     * @param page 页码
     * @param size 每页条数
     * @return 买到的商品列表
     */
    Result getBoughtGoods(Integer page, Integer size);

    /**
     * 获取我想要的商品列表（预购）
     * @param page 页码
     * @param size 每页条数
     * @param status 状态 (online/offline)
     * @return 预购商品列表
     */
    Result getBuyGoods(Integer page, Integer size, String status);
    /**
     * 编辑我的商品/预购需求
     * @param goodsId 商品ID
     * @param request 编辑请求
     * @return 编辑结果
     */
    Result editGoods(Long goodsId, GoodsEditRequest request);
    /**
     * 下架我的商品/预购需求
     * @param goodsId 商品ID
     * @return 下架结果
     */
    Result offlineGoods(Long goodsId);
    /**
     * 删除我的商品/预购需求
     * @param goodsId 商品ID
     * @return 删除结果
     */
    Result deleteGoods(Long goodsId);
}
