package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.TradeEvaluateSubmitRequest;
import com.bit.scmu_taotao.entity.TEvaluate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;

/**
* @author 35314
* @description 针对表【t_evaluate(交易评价表)】的数据库操作Service
* @createDate 2026-03-14 18:49:37
*/
public interface TEvaluateService extends IService<TEvaluate> {

    Result submitTradeEvaluate(@Valid TradeEvaluateSubmitRequest request);
}
