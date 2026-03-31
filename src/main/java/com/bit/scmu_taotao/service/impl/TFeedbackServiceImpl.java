package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackSubmitRequest;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackPageRequest;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackListItemResponse;
import com.bit.scmu_taotao.entity.TFeedback;
import com.bit.scmu_taotao.service.TFeedbackService;
import com.bit.scmu_taotao.mapper.TFeedbackMapper;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 35314
* @description 针对表【t_feedback(用户反馈建议表)】的数据库操作Service实现
* @createDate 2026-03-14 18:49:38
*/
@Slf4j
@Service
public class TFeedbackServiceImpl extends ServiceImpl<TFeedbackMapper, TFeedback>
    implements TFeedbackService{

    @Override
    public Result submitFeedback(FeedbackSubmitRequest request) {
                try {
                    String userId = UserContext.getUserId();
                    if (userId == null || userId.trim().isEmpty()) {
                           return Result.fail(401, "用户未登录或登录已过期");
                       }

                    TFeedback feedback = new TFeedback();
                    feedback.setUserId(userId);
                    feedback.setFeedbackContent(request.getContent().trim());
                    feedback.setFeedbackStatus(0);
                    feedback.setIsDelete(0);
                    boolean saved = this.save(feedback);
                        if (!saved || feedback.getFeedbackId() == null) {
                             log.warn("反馈提交失败：userId={}", userId);
                             return Result.fail("反馈提交失败，请稍后重试");
                        }
                        return Result.ok("反馈提交成功，我们将尽快回复", Map.of("feedbackId", feedback.getFeedbackId()));
                    } catch (Exception e) {
                        log.error("反馈提交异常：{}", e.getMessage(), e);
                        return Result.fail("反馈提交失败，请稍后重试");
                    }
            }

    @Override
    public Result getFeedbackList(FeedbackPageRequest query) {
        try {
            String userId = UserContext.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail(401, "用户未登录或登录已过期");
            }

            log.info("获取反馈列表：userId={}, page={}, size={}", userId, query.getPage(), query.getSize());

            // 分页查询
            Page<TFeedback> page = new Page<>(query.getPage(), query.getSize());
            LambdaQueryWrapper<TFeedback> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TFeedback::getUserId, userId)
                    .eq(TFeedback::getIsDelete, 0)
                    .orderByDesc(TFeedback::getCreateTime);

            Page<TFeedback> resultPage = this.page(page, queryWrapper);
            List<TFeedback> records = resultPage.getRecords();

            if (records.isEmpty()) {
                return Result.ok("请求成功", Map.of(
                        "total", 0L,
                        "pages", 0L,
                        "list", List.of()
                ));
            }

            // 转换响应格式
            List<FeedbackListItemResponse> list = records.stream()
                    .map(feedback -> {
                        FeedbackListItemResponse item = new FeedbackListItemResponse();
                        item.setId(feedback.getFeedbackId());
                        item.setContent(feedback.getFeedbackContent());
                        item.setSubmitTime(feedback.getCreateTime());
                        item.setReplyContent(feedback.getReplyContent());
                        item.setReplyTime(feedback.getReplyTime());
                        item.setStatus(feedback.getFeedbackStatus() == 0 ? "pending" : "processed");
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("获取反馈列表失败：{}", e.getMessage(), e);
            return Result.fail("获取反馈列表失败，请稍后重试");
        }
    }
}





