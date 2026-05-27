package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.dto.admin.AdminReportPageRequest;
import com.bit.scmu_taotao.dto.admin.AdminReportVerifyRequest;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.service.TUserReportService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-举报审核 Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final TUserReportService userReportService;

    public AdminReportController(TUserReportService userReportService) {
        this.userReportService = userReportService;
    }

    /**
     * 查询举报列表（管理员端）
     */
    @GetMapping
    public Result list(@Valid AdminReportPageRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("查询举报列表: keyword={}, page={}, size={}",
                request.getKeyword(), request.getPage(), request.getSize());
        return userReportService.getAdminReportList(request);
    }

    /**
     * 查看举报详情（管理员端）
     */
    @GetMapping("/{reportId}")
    public Result detail(@PathVariable Long reportId) {
        log.info("查询举报详情: reportId={}", reportId);
        return userReportService.getReportDetail(reportId);
    }

    /**
     * 处理举报（属实/驳回）
     */
    @PutMapping("/{reportId}/verify")
    public Result verify(@PathVariable Long reportId,
                         @RequestBody @Valid AdminReportVerifyRequest request,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("处理举报: reportId={}, action={}, deductScore={}",
                reportId, request.getAction(), request.getDeductScore());
        return userReportService.verifyReport(reportId, request);
    }
}

