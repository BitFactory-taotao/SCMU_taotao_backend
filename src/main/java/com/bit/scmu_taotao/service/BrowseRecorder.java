package com.bit.scmu_taotao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 商品浏览埋点异步记录器
 * 由 @Async + taskExecutor 线程池执行，埋点失败不影响主流程
 */
@Slf4j
@Component
public class BrowseRecorder {

    @Autowired
    private RecommendationService recommendationService;

    @Async("taskExecutor")
    public void recordBrowse(String userId, Long goodsId) {
        try {
            recommendationService.recordBrowseAndUpdateViewCount(userId, goodsId);
            log.debug("Browse logging completed: userId={}, goodsId={}", userId, goodsId);
        } catch (Exception e) {
            log.warn("Failed to record browse for goodsId={}, userId={}", goodsId, userId, e);
        }
    }
}
