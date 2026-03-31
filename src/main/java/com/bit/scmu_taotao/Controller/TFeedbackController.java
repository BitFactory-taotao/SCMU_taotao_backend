package com.bit.scmu_taotao.Controller;

import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackSubmitRequest;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackPageRequest;
import com.bit.scmu_taotao.service.TFeedbackService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TFeedbackController {
    @Autowired
    private TFeedbackService feedbackService;

    @PostMapping("/feedback")
    public Result submitFeedback(@Valid @RequestBody FeedbackSubmitRequest request,
                         BindingResult bindingResult) {
      if (bindingResult.hasErrors()) {
              String msg = bindingResult.getFieldError() != null
                              ? bindingResult.getFieldError().getDefaultMessage()
                               : "参数错误";
               return Result.fail(400, msg);
           }
      log.info("提交反馈请求：contentLength={}", request.getContent().length());
      return feedbackService.submitFeedback(request);
   }

    @GetMapping("/feedback")
    public Result getFeedbackList(@Valid FeedbackPageRequest query,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("获取反馈列表请求：page={}, size={}", query.getPage(), query.getSize());
        return feedbackService.getFeedbackList(query);
    }
}

