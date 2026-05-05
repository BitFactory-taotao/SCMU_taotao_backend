package com.bit.scmu_taotao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.dto.admin.AdminLoginRequest;
import com.bit.scmu_taotao.entity.TAdmin;
import com.bit.scmu_taotao.util.common.Result;

/**
 * @author 35314
 * @description 针对表【t_admin(后台管理员表)】的数据库操作 Service
 * @createDate 2026-05-04
 */
public interface TAdminService extends IService<TAdmin> {

    /**
     * 管理员登录
     *
     * @param request 登录请求
     * @return 登录结果
     */
    Result login(AdminLoginRequest request);

    /**
     * 获取当前登录管理员信息
     *
     * @return 管理员信息
     */
    Result getProfile();

    /**
     * 管理员退出登录
     *
     * @param authorization 请求头中的 Authorization
     * @return 退出结果
     */
    Result logout(String authorization);
}

