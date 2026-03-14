package com.bit.scmu_taotao.Controller;

import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器
 * 处理用户登录、登出等相关请求
 */
@RestController
@Slf4j
@RequestMapping("/user")
public class LoginController {

    @Autowired
    private TUserService tUserService;

    /**
     * 用户登录接口
     * @param userId 用户 ID（学号/工号）
     * @param password 密码
     * @return 登录结果，包含 token 和用户真实姓名
     */
    @PostMapping("/login")
    public Result login(@RequestParam("userId") String userId, @RequestParam("password") String password) {
        log.info("收到登录请求：userId={}", userId);

        // 参数校验
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.fail("密码不能为空");
        }

        // 调用 Service 层进行登录
        return tUserService.login(userId.trim(), password);
    }

    /**
     * 用户登出接口
     * @param token Token（从请求头获取）
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        log.info("收到登出请求：token={}", token != null ? "***" + token.substring(Math.max(0, token.length() - 8)) : "null");

        if (token == null || token.trim().isEmpty()) {
            return Result.fail("Token 不能为空");
        }

        // 去除可能的 "Bearer " 前缀
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        return tUserService.logout(cleanToken);
    }

    /**
     * 获取当前登录用户信息
     * @param token Token（从请求头获取）
     * @return 用户信息
     */
    @GetMapping("/userInfo")
    public Result getUserInfo(@RequestHeader("Authorization") String token) {
        log.info("获取用户信息请求");

        if (token == null || token.trim().isEmpty()) {
            return Result.fail("Token 不能为空");
        }

        // 去除可能的 "Bearer " 前缀
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        return tUserService.getUserInfoByToken(cleanToken);
    }
}
