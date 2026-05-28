package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.dto.admin.RiskHandleRequest;
import com.bit.scmu_taotao.dto.admin.RiskUserPageRequest;
import com.bit.scmu_taotao.service.AdminRiskUserService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-风险账号审核 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/users/risk")
public class AdminRiskUserController {

    private final AdminRiskUserService adminRiskUserService;

    public AdminRiskUserController(AdminRiskUserService adminRiskUserService) {
        this.adminRiskUserService = adminRiskUserService;
    }

    /**
     * 查询风险用户待办列表
     */
    @GetMapping("/list")
    public Result list(@Valid RiskUserPageRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("查询风险用户列表: keyword={}, page={}, pageSize={}", request.getKeyword(), request.getPage(), request.getPageSize());
        return adminRiskUserService.getRiskUserList(request);
    }

    /**
     * 查询风险账号多维详情
     */
    @GetMapping("/{userId}/metrics")
    public Result metrics(@PathVariable String userId) {
        log.info("查询风险账号详情: userId={}", userId);
        return adminRiskUserService.getRiskMetrics(userId);
    }

    /**
     * 查封或消除风险账号
     */
    @PutMapping("/handle")
    public Result handle(@RequestBody(required = false) @Valid RiskHandleRequest request, BindingResult bindingResult) {
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("处理风险账号: userIds={}, action={}", request.getUserIds(), request.getAction());
        return adminRiskUserService.handleRiskUsers(request);
    }
}

