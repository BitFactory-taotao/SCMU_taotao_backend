package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackListItemResponse;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackPageRequest;
import com.bit.scmu_taotao.dto.FeedbackDto.FeedbackSubmitRequest;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackDetailDTO;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackListItemDTO;
import com.bit.scmu_taotao.dto.admin.AdminFeedbackPageRequest;
import com.bit.scmu_taotao.entity.ChatMessage;
import com.bit.scmu_taotao.entity.ChatSession;
import com.bit.scmu_taotao.entity.TFeedback;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.mapper.TFeedbackMapper;
import com.bit.scmu_taotao.service.ChatSessionService;
import com.bit.scmu_taotao.service.StompPushService;
import com.bit.scmu_taotao.service.TFeedbackService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 35314
 * @description 针对表【t_feedback(用户反馈建议表)】的数据库操作Service实现
 * @createDate 2026-03-14 18:49:38
 */
@Slf4j
@Service
public class TFeedbackServiceImpl extends ServiceImpl<TFeedbackMapper, TFeedback>
        implements TFeedbackService {
    private final TUserService tUserService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageMapper chatMessageMapper;
    private final StompPushService stompPushService;

    public TFeedbackServiceImpl(TUserService tUserService,
                                ChatSessionService chatSessionService,
                                ChatMessageMapper chatMessageMapper,
                                StompPushService stompPushService) {
        this.tUserService = tUserService;
        this.chatSessionService = chatSessionService;
        this.chatMessageMapper = chatMessageMapper;
        this.stompPushService = stompPushService;
    }

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

    @Override
    public Result getAdminFeedbackList(AdminFeedbackPageRequest query) {
        try {
            Page<TFeedback> page = new Page<>(query.getPage(), query.getSize());
            LambdaQueryWrapper<TFeedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TFeedback::getIsDelete, 0)
                    .orderByDesc(TFeedback::getCreateTime);

            if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
                String s = query.getStatus().trim();
                if ("pending".equalsIgnoreCase(s)) {
                    wrapper.eq(TFeedback::getFeedbackStatus, 0);
                } else if ("processed".equalsIgnoreCase(s)) {
                    wrapper.eq(TFeedback::getFeedbackStatus, 1);
                }
            }

            if (StringUtils.isNotBlank(query.getKeyword())) {
                String kw = query.getKeyword().trim();
                // try to find user ids matching userName
                QueryWrapper<TUser> userQ = new QueryWrapper<>();
                userQ.select("user_id").like("user_name", kw);
                List<TUser> users = tUserService.list(userQ);
                List<String> userIds = users.stream().map(TUser::getUserId).collect(Collectors.toList());

                // 始终搜索反馈内容，如果有匹配的用户名则加入 OR 条件
                wrapper.and(w -> {
                    w.like(TFeedback::getFeedbackContent, kw);
                    if (!userIds.isEmpty()) {
                        w.or().in(TFeedback::getUserId, userIds);
                    }
                });
            }

            Page<TFeedback> resultPage = this.page(page, wrapper);
            Set<String> userIds = resultPage.getRecords().stream().map(TFeedback::getUserId).collect(Collectors.toSet());
            Map<String, TUser> userMap = userIds.isEmpty()
                    ? Collections.emptyMap()
                    : tUserService.listByIds(userIds).stream().collect(Collectors.toMap(TUser::getUserId, u -> u));
            List<AdminFeedbackListItemDTO> list = resultPage.getRecords().stream().map(feedback -> {
                AdminFeedbackListItemDTO item = new AdminFeedbackListItemDTO();
                item.setId(String.valueOf(feedback.getFeedbackId()));
                item.setUserId(feedback.getUserId());
                TUser user = userMap.get(feedback.getUserId());
                if (user != null) {
                    item.setUserName(user.getUserName());
                    item.setAvatar(user.getAvatar());
                }
                item.setContent(feedback.getFeedbackContent());
                item.setSubmitTime(feedback.getCreateTime());
                item.setStatus(feedback.getFeedbackStatus() == null || feedback.getFeedbackStatus() == 0 ? "pending" : "processed");
                item.setIs_read(feedback.getIsRead() != null && feedback.getIsRead() == 1 ? "read" : "unread");
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("获取管理员反馈列表失败：{}", e.getMessage(), e);
            return Result.fail("获取管理员反馈列表失败，请稍后重试");
        }
    }

    @Override
    public Result getFeedbackDetail(Long feedbackId) {
        try {
            TFeedback feedback = this.getById(feedbackId);
            if (feedback == null) {
                return Result.fail(404, "反馈未找到");
            }
            TUser user = tUserService.getById(feedback.getUserId());
            AdminFeedbackDetailDTO dto = new AdminFeedbackDetailDTO();
            dto.setId(String.valueOf(feedback.getFeedbackId()));
            dto.setUserId(feedback.getUserId());
            if (user != null) {
                dto.setUserName(user.getUserName());
                dto.setAvatar(user.getAvatar());
            }
            dto.setSubmitTime(feedback.getCreateTime());
            dto.setContent(feedback.getFeedbackContent());
            dto.setReplyContent(feedback.getReplyContent());
            dto.setStatus(feedback.getFeedbackStatus() == null || feedback.getFeedbackStatus() == 0 ? "pending" : "processed");
            dto.setIs_read(feedback.getIsRead() != null && feedback.getIsRead() == 1 ? "read" : "unread");

            // mark as read
            if (feedback.getIsRead() == null || feedback.getIsRead() == 0) {
                TFeedback upd = new TFeedback();
                upd.setFeedbackId(feedbackId);
                upd.setIsRead(1);
                this.updateById(upd);
                dto.setIs_read("read");
            }

            return Result.ok("请求成功", dto);
        } catch (Exception e) {
            log.error("获取反馈详情失败：{}", e.getMessage(), e);
            return Result.fail("获取反馈详情失败，请稍后重试");
        }
    }

    @Override
    public Result markUnread(List<Long> feedbackIds) {
        try {
            if (feedbackIds == null || feedbackIds.isEmpty()) {
                return Result.fail(400, "feedbackIds不能为空");
            }
            TFeedback t = new TFeedback();
            t.setIsRead(0);
            UpdateWrapper<TFeedback> uw = new UpdateWrapper<>();
            uw.in("feedback_id", feedbackIds);
            int affected = this.baseMapper.update(t, uw);
            return Result.ok(String.format("操作成功，已标记 %d 条反馈为未读", affected), null);
        } catch (Exception e) {
            log.error("标记未读失败：{}", e.getMessage(), e);
            return Result.fail("标记未读失败，请稍后重试");
        }
    }

    @Override
    public Result resolveFeedback(Long feedbackId, String replyContent) {
        try {
            TFeedback feedback = new TFeedback();
            feedback.setFeedbackId(feedbackId);
            feedback.setReplyContent(replyContent);
            feedback.setReplyTime(LocalDateTime.now());
            feedback.setFeedbackStatus(1);

            // 仅更新待处理的反馈（feedbackStatus == 0），防止并发冲突
            UpdateWrapper<TFeedback> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("feedback_id", feedbackId)
                    .eq("feedback_status", 0);

            int affected = this.baseMapper.update(feedback, updateWrapper);

            if (affected == 0) {
                // 检查反馈是否存在或已被处理
                TFeedback existing = this.getById(feedbackId);
                if (existing == null) {
                    return Result.fail(404, "反馈未找到");
                } else {
                    return Result.fail(409, "该反馈已被处理或处理中，请刷新后重试");
                }
            }
            TFeedback resolvedFeedback = this.getById(feedbackId);
            if (resolvedFeedback != null && resolvedFeedback.getUserId() != null) {
                String targetUserId = resolvedFeedback.getUserId();
                // 1) 查找是否已有 system <-> user 的会话（复用）
                ChatSession session = chatSessionService.findSessionByUsers("system", targetUserId);
                Date now = new Date();
                if (session == null) {
                    // 2) 没有则创建新会话
                    session = new ChatSession();
                    session.setUser1Id("system");
                    session.setUser2Id(targetUserId);
                    session.setStatus(1);
                    session.setLastTime(now);
                    chatSessionService.save(session); // 保存后 session.chatId 应被填充
                } else {
                    // 3) 确保会话为正常状态并更新 lastTime
                    if (!Integer.valueOf(1).equals(session.getStatus())) {
                        session.setStatus(1);
                    }
                    session.setLastTime(now);
                    chatSessionService.updateById(session);
                }

                // 4) 构建并保存系统消息（关联到会话）
                ChatMessage message = new ChatMessage();
                message.setChatId(session.getChatId());
                message.setSendId("system");
                message.setReceiveId(targetUserId);
                message.setMsgType(0);
                message.setMsgContent("您的反馈已处理。管理员回复：" + replyContent);
                message.setIsRead(0);
                message.setIsDelete(0);
                chatMessageMapper.insert(message);

                // 5) 更新会话的 lastMsg/lastTime（把系统消息作为最后消息）
                ChatSession updateSession = new ChatSession();
                updateSession.setChatId(session.getChatId());
                updateSession.setLastMsg(message.getMsgContent());
                updateSession.setLastTime(now);
                chatSessionService.updateById(updateSession);

                // 6) 推送通知（保留原有推送逻辑）
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "system");
                payload.put("content", message.getMsgContent());
                payload.put("feedbackId", feedbackId);
                payload.put("createTime", message.getCreateTime());
                try {
                    stompPushService.pushToUserQueue(targetUserId, "/queue/messages", payload);
                    log.info("反馈处理成功并已通知用户：feedbackId={}, userId={}", feedbackId, targetUserId);
                } catch (Exception e) {
                    log.warn("反馈已处理但实时推送失败（消息已入库，用户下次登录可见）：feedbackId={}, userId={}, error={}",
                            feedbackId, targetUserId, e.getMessage());
                }
            } else {
                log.warn("反馈处理成功但未找到有效反馈提交者，跳过通知：feedbackId={}", feedbackId);
            }
            return Result.ok("反馈已处理，结果已通知用户", null);
        } catch (
                Exception e) {
            log.error("处理反馈失败：{}", e.getMessage(), e);
            return Result.fail("处理反馈失败，请稍后重试");
        }
    }
}




