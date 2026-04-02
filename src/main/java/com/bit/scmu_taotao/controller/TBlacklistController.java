package com.bit.scmu_taotao.controller;

import com.bit.scmu_taotao.dto.BlacklistPageQuery;
import com.bit.scmu_taotao.service.TBlacklistService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class TBlacklistController {

    @Autowired
    private TBlacklistService blacklistService;

    /**
     * 加入黑名单
     *
     * @param userId 被拉黑用户ID（学号/工号）
     * @return 操作结果
     */
    @PostMapping("/{userId}/blacklist")
    public Result addBlacklist(@PathVariable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail(400, "用户ID不能为空");
        }
        log.info("加入黑名单请求：userId={}", userId);
        return blacklistService.addBlacklist(userId);
    }

    /**
     * 获取黑名单列表（分页）
     *
     * @param query         分页参数：page(默认1)、size(默认10，最大100)
     * @param bindingResult 参数校验结果
     * @return 黑名单列表，包含total/pages/list(studentId/name/addTime)
     */
    @GetMapping("/blacklist")
    public Result getBlacklist(@Valid BlacklistPageQuery query, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("获取黑名单列表请求：page={}, size={}", query.getPage(), query.getSize());
        return blacklistService.getBlacklistPage(query.getPage(), query.getSize());
    }

    /**
     * 解除黑名单
     *
     * @param userId 被拉黑用户ID（学号/工号）
     * @return 操作结果
     */
    @DeleteMapping("/{userId}/blacklist")
    public Result removeBlacklist(@PathVariable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail(400, "用户ID不能为空");
        }
        log.info("解除黑名单请求：userId={}", userId);
        return blacklistService.removeBlacklist(userId);
    }
}
