package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.dto.admin.*;
import com.bit.scmu_taotao.service.AdminSolvedItemsService;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/admin/solved-items")
public class AdminSolvedItemsController {

    @Autowired
    private AdminSolvedItemsService solvedItemsService;

    @GetMapping("/list")
    public Result list(@Valid SolvedItemListRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage() : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("查询解决事项列表: type={}, status={}, keyword={}, page={}, size={}",
                request.getType(), request.getStatus(), request.getKeyword(), request.getPage(), request.getSize());
        return solvedItemsService.getSolvedItemList(request);
    }

    @GetMapping("/detail")
    public Result detail(@RequestParam String type, @RequestParam String id) {
        log.info("查询解决事项详情: type={}, id={}", type, id);
        return solvedItemsService.getSolvedItemDetail(type, id);
    }

    @GetMapping("/count")
    public Result count() {
        log.info("查询解决事项统计数量");
        return solvedItemsService.getSolvedItemCount();
    }

    @PutMapping("/revoke")
    public Result revoke(@RequestBody(required = false) @Valid SolvedItemRevokeRequest request,
                         BindingResult bindingResult) {
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage() : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("撤销解决事项: type={}, id={}", request.getType(), request.getId());
        return solvedItemsService.revokeSolvedItem(request);
    }
}

