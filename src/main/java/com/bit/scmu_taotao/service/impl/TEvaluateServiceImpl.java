package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.TradeEvaluateSubmitRequest;
import com.bit.scmu_taotao.entity.TEvaluate;
import com.bit.scmu_taotao.entity.TEvaluateImage;
import com.bit.scmu_taotao.entity.TTrade;
import com.bit.scmu_taotao.service.TEvaluateImageService;
import com.bit.scmu_taotao.service.TEvaluateService;
import com.bit.scmu_taotao.mapper.TEvaluateMapper;
import com.bit.scmu_taotao.service.TTradeService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author 35314
* @description 针对表【t_evaluate(交易评价表)】的数据库操作Service实现
* @createDate 2026-03-14 18:49:37
*/
@Slf4j
@Service
public class TEvaluateServiceImpl extends ServiceImpl<TEvaluateMapper, TEvaluate>
    implements TEvaluateService{
    @Autowired
    private TTradeService tradeService;
    @Autowired
    private TEvaluateImageService evaluateImageService;

    @Transactional
    @Override
    public Result submitTradeEvaluate(TradeEvaluateSubmitRequest request) {
        try {
            String buyerId = UserContext.getUserId();
            if (buyerId == null || buyerId.trim().isEmpty()) {
                return Result.fail(401, "用户未登录或登录已过期");
            }

            if (request == null) {
                return Result.fail(400, "请求参数不能为空");
            }

            if (request.getSellerId() != null && request.getSellerId().trim().equals(buyerId)) {
                return Result.fail(400, "不能评价自己");
            }

            log.info("提交交易评价：buyerId={}, goodsId={}, sellerId={}",
                    buyerId, request.getGoodsId(), request.getSellerId());

            // 根据商品+卖家+买家定位交易
            TTrade trade = tradeService.getOne(new LambdaQueryWrapper<TTrade>()
                    .eq(TTrade::getGoodsId, request.getGoodsId())
                    .eq(TTrade::getSellerId, request.getSellerId())
                    .eq(TTrade::getBuyerId, buyerId)
                    .eq(TTrade::getIsDelete, 0)
                    .orderByDesc(TTrade::getTradeTime)
                    .orderByDesc(TTrade::getCreateTime)
                    .last("limit 1"), false);

            if (trade == null) {
                return Result.fail(404, "未找到对应交易记录");
            }

            TEvaluate evaluate = new TEvaluate();
            evaluate.setTradeId(trade.getTradeId());
            evaluate.setGoodsId(request.getGoodsId());
            evaluate.setBuyerId(buyerId);
            evaluate.setSellerId(request.getSellerId().trim());
            evaluate.setDescScore(request.getGoodsDescScore());
            evaluate.setCommScore(request.getCommunicateScore());
            evaluate.setTotalScore(request.getTotalScore());

            String content = request.getContent();
            evaluate.setEvalContent(content == null ? null : content.trim());

            evaluate.setIsAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()) ? 1 : 0);
            evaluate.setIsDelete(0);

            boolean saved = this.save(evaluate);
            if (!saved || evaluate.getEvalId() == null) {
                log.warn("评价主记录保存失败：buyerId={}, tradeId={}", buyerId, trade.getTradeId());
                return Result.fail("评价提交失败，请稍后重试");
            }

            // 保存评价图片（选填）
            List<String> imgUrls = request.getImgUrls();
            if (imgUrls != null && !imgUrls.isEmpty()) {
                List<TEvaluateImage> imageList = imgUrls.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(url -> !url.isEmpty())
                        .distinct()
                        .map(url -> {
                            TEvaluateImage image = new TEvaluateImage();
                            image.setEvalId(evaluate.getEvalId());
                            image.setImgUrl(url);
                            return image;
                        })
                        .collect(Collectors.toList());

                if (!imageList.isEmpty()) {
                    boolean imageSaved = evaluateImageService.saveBatch(imageList);
                    if (!imageSaved) {
                        throw new RuntimeException("评价图片保存失败");
                    }
                }
            }

            log.info("交易评价提交成功：evalId={}, tradeId={}, buyerId={}",
                    evaluate.getEvalId(), trade.getTradeId(), buyerId);
            return Result.ok("评价提交成功", null);
        } catch (Exception e) {
            // 发生异常时显式标记事务回滚，避免主表已写入但附图失败
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("交易评价提交失败：{}", e.getMessage(), e);
            return Result.fail("评价提交失败，请稍后重试");
        }
    }
}




