package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackPageRequest;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackSubmitRequest;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackPageRequest;
import com.bit.scmu_taotao.entity.TFeedback;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;

import java.util.List;

/**
* @author 35314
* @description 针对表【t_feedback(用户反馈建议表)】的数据库操作Service
* @createDate 2026-03-14 18:49:38
*/
public interface TFeedbackService extends IService<TFeedback> {
    Result submitFeedback(@Valid FeedbackSubmitRequest request);

    Result getFeedbackList(FeedbackPageRequest query);

    // Admin methods
    Result getAdminFeedbackList(AdminFeedbackPageRequest query);

    Result getFeedbackDetail(Long feedbackId);

    Result markUnread(List<Long> feedbackIds);

    Result resolveFeedback(Long feedbackId, String replyContent);
}
