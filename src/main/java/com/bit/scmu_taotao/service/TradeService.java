package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.util.common.Result;

/**
 * 交易工作流服务
 * 承接买家发起交易、撤回交易、卖家拒绝/确认成交等交易意图编排
 */
public interface TradeService {

    Result initiateTrade(Long goodsId, String userId);

    Result withdrawTradeRequest(Long goodsId, String buyerId);

    Result rejectTradeRequest(Long goodsId, String buyerId, String sellerId);

    Result confirmTrade(Long goodsId, String buyerId, String sellerId);
}
