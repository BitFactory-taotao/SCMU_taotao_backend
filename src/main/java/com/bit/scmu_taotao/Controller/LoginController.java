package com.bit.scmu_taotao.Controller;

import com.bit.scmu_taotao.dto.LoginRequest;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.UserContext;
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
     * @param request 登录请求，包含用户名和密码
     * @return 登录结果，包含 token 和用户真实姓名
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        log.info("收到登录请求：userId={}", request.getUserId());

        // 参数校验
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return Result.fail("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.fail("密码不能为空");
        }

        // 调用 Service 层进行登录
        return tUserService.login(request.getUserId().trim(), request.getPassword());
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
     * @return 用户信息
     */
    @GetMapping("/userInfo")
    public Result getUserInfo() {
        // 从 UserContext 获取当前登录用户 ID（由拦截器填充）
        String userId = UserContext.getUserId();
        log.info("获取用户信息请求：userId={}", userId);

        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail(401, "用户未登录");
        }

        // 通过 userId 获取完整信息
        TUser user = tUserService.getById(userId);
        if (user == null) {
            return Result.fail("用户信息不存在");
        }
        return Result.ok(user);
    }
}
