package com.bit.scmu_taotao.controller.admin;

import com.bit.scmu_taotao.dto.admin.AdminFeedbackPageRequest;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackMarkUnreadRequest;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackResolveRequest;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.service.TFeedbackService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {

    private final TFeedbackService feedbackService;

    public AdminFeedbackController(TFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/list")
    public Result list(@Valid AdminFeedbackPageRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("查询反馈列表: keyword={}, status={}, page={}, size={}", request.getKeyword(), request.getStatus(), request.getPage(), request.getSize());
        return feedbackService.getAdminFeedbackList(request);
    }

    @GetMapping("/{feedbackId}")
    public Result detail(@PathVariable Long feedbackId) {
        return feedbackService.getFeedbackDetail(feedbackId);
    }

    @PutMapping("/mark-unread")
    public Result markUnread(@RequestBody @Valid AdminFeedbackMarkUnreadRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        return feedbackService.markUnread(request.getFeedbackIds());
    }

    @PostMapping("/{feedbackId}/resolve")
    public Result resolve(@PathVariable Long feedbackId, @RequestBody @Valid AdminFeedbackResolveRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        return feedbackService.resolveFeedback(feedbackId, request.getReplyContent());
    }
}
