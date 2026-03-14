package com.bit.scmu_taotao.service;

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
}
