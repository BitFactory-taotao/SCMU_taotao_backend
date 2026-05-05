package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.dto.admin.AdminLoginRequest;
import com.bit.scmu_taotao.service.TAdminService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private TAdminService adminService;

    @PostMapping("/login")
    public Result login(@Valid @RequestBody(required = false) AdminLoginRequest request, BindingResult bindingResult) {
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ? bindingResult.getFieldError().getDefaultMessage() : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("管理员登录接口：adminId={}", request.getAdminId());
        return adminService.login(request);
    }

    @GetMapping("/auth/profile")
    public Result profile() {
        return adminService.getProfile();
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return adminService.logout(authorization);
    }
}

