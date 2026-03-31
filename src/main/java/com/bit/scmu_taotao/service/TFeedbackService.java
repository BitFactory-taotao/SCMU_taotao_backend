package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackSubmitRequest;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackPageRequest;
import com.bit.scmu_taotao.entity.TFeedback;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;

/**
* @author 35314
* @description 针对表【t_feedback(用户反馈建议表)】的数据库操作Service
* @createDate 2026-03-14 18:49:38
*/
public interface TFeedbackService extends IService<TFeedback> {

    Result submitFeedback(@Valid FeedbackSubmitRequest request);

    Result getFeedbackList(FeedbackPageRequest query);
}
