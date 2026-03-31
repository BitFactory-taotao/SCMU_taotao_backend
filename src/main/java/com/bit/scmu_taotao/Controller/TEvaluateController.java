package com.bit.scmu_taotao.Controller;

import com.bit.scmu_taotao.dto.TradeEvaluateSubmitRequest;
import com.bit.scmu_taotao.service.TEvaluateService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TEvaluateController {
    @Autowired
    private TEvaluateService evaluateService;

    @PostMapping("/evaluate/trade")
    public Result submitTradeEvaluate(@Valid @RequestBody TradeEvaluateSubmitRequest request,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }
        log.info("提交交易评价请求：goodsId={}, sellerId={}", request.getGoodsId(), request.getSellerId());
        return evaluateService.submitTradeEvaluate(request);
    }
}
