package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.dto.admin.GoodsAuditApproveRequest;
import com.bit.scmu_taotao.dto.admin.GoodsAuditPageRequest;
import com.bit.scmu_taotao.dto.admin.GoodsAuditRejectRequest;
import com.bit.scmu_taotao.service.TGoodsService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品巡检审核接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("admin/goods/audit")
public class AdminGoodsAuditController {

    @Autowired
    private TGoodsService tGoodsService;

    @GetMapping("/list")
    public Result list(@Valid GoodsAuditPageRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ? bindingResult.getFieldError().getDefaultMessage() : "参数错误";
            return Result.fail(400, msg);
        }

        log.info("查询商品巡检列表：auditStatus={}, category={}, keyword={}, page={}, size={}", request.getAuditStatus(), request.getCategory(), request.getKeyword(), request.getPage(), request.getSize());
        return tGoodsService.getAuditGoodsList(request.getAuditStatus(), request.getCategory(), request.getKeyword(), request.getPage(), request.getSize());
    }

    @GetMapping("/{goodsId}")
    public Result detail(@PathVariable("goodsId") @Min(value = 1, message = "goodsId必须大于0") Long goodsId) {
        log.info("查询商品巡检详情：goodsId={}", goodsId);
        return tGoodsService.getAuditGoodsDetail(goodsId);
    }

    @PutMapping("/approve")
    public Result approve(@Valid @RequestBody(required = false) GoodsAuditApproveRequest request, BindingResult bindingResult) {
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ? bindingResult.getFieldError().getDefaultMessage() : "参数错误";
            return Result.fail(400, msg);
        }

        log.info("商品巡检通过：goodsIds={}", request.getGoodsIds());
        return tGoodsService.approveAuditGoods(request.getGoodsIds());
    }

    @PutMapping("/reject")
    public Result reject(@Valid @RequestBody(required = false) GoodsAuditRejectRequest request, BindingResult bindingResult) {
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ? bindingResult.getFieldError().getDefaultMessage() : "参数错误";
            return Result.fail(400, msg);
        }

        log.info("商品巡检驳回：goodsIds={}, reason={}", request.getGoodsIds(), request.getReason());
        return tGoodsService.rejectAuditGoods(request.getGoodsIds(), request.getReason());
    }
}
