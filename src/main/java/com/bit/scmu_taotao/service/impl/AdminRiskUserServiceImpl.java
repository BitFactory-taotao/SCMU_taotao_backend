package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.admin.RiskHandleRequest;
import com.bit.scmu_taotao.dto.admin.RiskMetricsDTO;
import com.bit.scmu_taotao.dto.admin.RiskUserListItemDTO;
import com.bit.scmu_taotao.dto.admin.RiskUserPageRequest;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.mapper.TAccountAuditLogMapper;
import com.bit.scmu_taotao.mapper.TUserMapper;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理员-风险账号审核服务实现
 */
@Slf4j
@Service
public class AdminRiskUserServiceImpl extends ServiceImpl<TUserMapper, TUser>
        implements AdminRiskUserService {

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TUserReportService tUserReportService;

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TBlacklistService tBlacklistService;

    @Autowired
    private TEvaluateService tEvaluateService;

    @Autowired
    private TCreditLogService creditLogService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private StompPushService stompPushService;

    @Autowired
    private TAccountAuditLogMapper tAccountAuditLogMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result getRiskUserList(RiskUserPageRequest request) {
        try {
            String keyword = request.getKeyword();
            if (StringUtils.hasText(keyword)) {
                keyword = keyword.trim();
            }

            LambdaQueryWrapper<TUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(TUser::getCreditScore, 70)
                    .eq(TUser::getStatus, 2)
                    .eq(TUser::getIsDelete, 0);
            if (StringUtils.hasText(keyword)) {
                String finalKeyword = keyword;
                wrapper.and(w -> w.like(TUser::getUserName, finalKeyword)
                        .or().like(TUser::getUserId, finalKeyword));
            }
            wrapper.orderByDesc(TUser::getCreateTime);

            Page<TUser> page = new Page<>(request.getPage(), request.getPageSize());
            Page<TUser> resultPage = tUserService.page(page, wrapper);

            List<RiskUserListItemDTO> list = resultPage.getRecords().stream().map(user -> {
                RiskUserListItemDTO dto = new RiskUserListItemDTO();
                dto.setUserId(user.getUserId());
                dto.setUserName(user.getUserName());
                dto.setAvatar(user.getAvatar());
                dto.setRiskLevel(resolveRiskLevel(user.getCreditScore()));
                dto.setRegisterTime(user.getCreateTime() == null ? "" : user.getCreateTime().format(DATETIME_FORMATTER));
                return dto;
            }).collect(Collectors.toList());

            return Result.ok(list, resultPage.getTotal());
        } catch (Exception e) {
            log.error("查询风险用户列表失败：{}", e.getMessage(), e);
            return Result.fail("查询风险用户列表失败，请稍后重试");
        }
    }

    @Override
    public Result getRiskMetrics(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                return Result.fail(400, "userId不能为空");
            }

            TUser user = tUserService.getById(userId);
            if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
                return Result.fail(404, "用户不存在");
            }

            long reportCount = tUserReportService.count(new LambdaQueryWrapper<TUserReport>()
                    .eq(TUserReport::getTargetId, userId)
                    .eq(TUserReport::getStatus, 1));
            long itemViolationCount = tGoodsService.count(new LambdaQueryWrapper<TGoods>()
                    .eq(TGoods::getUserId, userId)
                    .eq(TGoods::getIsAudited, 2)
                    .eq(TGoods::getIsDelete, 0));
            long blacklistCount = tBlacklistService.count(new LambdaQueryWrapper<TBlacklist>()
                    .eq(TBlacklist::getBlackUserId, userId)
                    .eq(TBlacklist::getIsDelete, 0));
            long langViolationCount = tUserReportService.count(new LambdaQueryWrapper<TUserReport>()
                    .eq(TUserReport::getTargetId, userId)
                    .eq(TUserReport::getStatus, 1)
                    .eq(TUserReport::getTag, "LANG_VIOLATION"));
            long lowRatingCount = tEvaluateService.count(new LambdaQueryWrapper<TEvaluate>()
                    .eq(TEvaluate::getSellerId, userId)
                    .lt(TEvaluate::getTotalScore, 40)
                    .eq(TEvaluate::getIsDelete, 0));

            RiskMetricsDTO dto = new RiskMetricsDTO();
            dto.setUserId(user.getUserId());
            dto.setUserName(user.getUserName());
            dto.setAvatar(user.getAvatar());
            dto.setCreditScore(user.getCreditScore());
            dto.setCreditStar(user.getCreditStar());

            RiskMetricsDTO.Metrics metrics = new RiskMetricsDTO.Metrics();
            metrics.setReportCount(reportCount);
            metrics.setItemViolationCount(itemViolationCount);
            metrics.setBlacklistCount(blacklistCount);
            metrics.setLangViolationCount(langViolationCount);
            metrics.setLowRatingCount(lowRatingCount);
            dto.setMetrics(metrics);

            return Result.ok(dto);
        } catch (Exception e) {
            log.error("查询风险账号多维详情失败：userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("查询风险账号多维详情失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result handleRiskUsers(RiskHandleRequest request) {
        try {
            if (request == null) {
                return Result.fail(400, "请求体不能为空");
            }

            String action = request.getAction();
            String reason = request.getReason();
            if (StringUtils.hasText(reason)) {
                reason = reason.trim();
            } else {
                reason = null;
            }

            List<String> userIds = request.getUserIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) {
                return Result.fail(400, "userIds不能为空");
            }

            int successCount = 0;
            for (String userId : userIds) {
                TUser targetUser = tUserService.getById(userId);
                if (targetUser == null || Integer.valueOf(1).equals(targetUser.getIsDelete())) {
                    log.warn("风险账号处理跳过不存在的用户：userId={}", userId);
                    continue;
                }

                String messageContent;
                TCreditLog creditLog = new TCreditLog();
                creditLog.setUserId(userId);

                if ("BAN".equals(action)) {
                    tUserService.update(new LambdaUpdateWrapper<TUser>()
                            .eq(TUser::getUserId, userId)
                            .set(TUser::getStatus, 1)
                            .set(TUser::getViolationReason, reason));
                    messageContent = buildBanMessage(reason);
                    creditLog.setScoreChange(0);
                    creditLog.setChangeType("RISK_BAN");
                    creditLog.setReason(StringUtils.hasText(reason) ? "风险账号查封：" + reason : "风险账号查封");
                } else {
                    Integer originalScore = targetUser.getCreditScore();
                    int newScore = Math.max(originalScore != null ? originalScore : 0, 80);
                    messageContent = "您的账号风险已解除，当前信誉分已恢复至" + newScore + "分。";
                    creditLog.setScoreChange(originalScore == null ? 0 : newScore - originalScore);
                    creditLog.setChangeType("RISK_CLEAR");
                    creditLog.setReason("风险账号消除，信誉分恢复至80分");

                    // 使用 UpdateWrapper 显式置空 violationReason（MyBatis-Plus 默认跳过 null）
                    tUserService.update(new LambdaUpdateWrapper<TUser>()
                            .eq(TUser::getUserId, userId)
                            .set(TUser::getStatus, 0)
                            .set(TUser::getCreditScore, newScore)
                            .set(TUser::getViolationReason, null));
                }

                creditLogService.save(creditLog);

                // 记录账号审核操作日志
                TAccountAuditLog auditLog = new TAccountAuditLog();
                auditLog.setUserId(userId);
                auditLog.setAction("BAN".equals(action) ? "ban" : "clear");
                auditLog.setPreviousStatus(targetUser.getStatus());
                auditLog.setReason(reason);
                auditLog.setIsDelete(0);
                tAccountAuditLogMapper.insert(auditLog);

                sendSystemMessage(userId, messageContent);
                successCount++;
            }

            return Result.ok("操作成功 " + successCount + " 条数据");
        } catch (Exception e) {
            log.error("处理风险账号失败：{}", e.getMessage(), e);
            // 确保事务回滚，避免部分用户状态更新导致数据不一致
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail("处理风险账号失败，请稍后重试");
        }
    }

    /**
     * 计算风险等级
     */
    private String resolveRiskLevel(Integer creditScore) {
        if (creditScore == null) {
            return "未知";
        }
        if (creditScore >= 60 && creditScore <= 69) {
            return "低度风险";
        }
        if (creditScore >= 40 && creditScore <= 59) {
            return "中度风险";
        }
        if (creditScore >= 0 && creditScore <= 39) {
            return "高度风险";
        }
        return "未知";
    }

    /**
     * 发送查封通知文案
     */
    private String buildBanMessage(String reason) {
        if (StringUtils.hasText(reason)) {
            return "您的账号因风险审核已被查封，原因：" + reason + "。";
        }
        return "您的账号因风险审核已被查封，请尽快联系平台处理。";
    }

    /**
     * 发送系统通知消息（与举报审核保持同样的 system↔user 会话模式）
     * 1. 查找/创建 system↔user 会话
     * 2. 保存 chat_message（msgType=0, sendId=system）
     * 3. 更新会话 lastMsg/lastTime
     * 4. STOMP 推送到 /queue/messages
     */
    private void sendSystemMessage(String targetUserId, String content) {
        try {
            ChatSession session = chatSessionService.findSessionByUsers("system", targetUserId);
            Date now = new Date();
            if (session == null) {
                session = new ChatSession();
                session.setUser1Id("system");
                session.setUser2Id(targetUserId);
                session.setStatus(1);
                session.setLastTime(now);
                chatSessionService.save(session);
            } else {
                if (!Integer.valueOf(1).equals(session.getStatus())) {
                    session.setStatus(1);
                }
                session.setLastTime(now);
                chatSessionService.updateById(session);
            }

            ChatMessage message = new ChatMessage();
            message.setChatId(session.getChatId());
            message.setSendId("system");
            message.setReceiveId(targetUserId);
            message.setMsgType(0);
            message.setMsgContent(content);
            message.setIsRead(0);
            message.setIsDelete(0);
            chatMessageMapper.insert(message);

            ChatSession updateSession = new ChatSession();
            updateSession.setChatId(session.getChatId());
            updateSession.setLastMsg(content);
            updateSession.setLastTime(now);
            chatSessionService.updateById(updateSession);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "system");
            payload.put("content", content);
            payload.put("createTime", message.getCreateTime());
            stompPushService.pushToUserQueue(targetUserId, "/queue/messages", payload);

            log.info("系统通知发送成功：targetUserId={}, content={}", targetUserId, content);
        } catch (Exception e) {
            log.warn("系统通知发送失败：targetUserId={}", targetUserId, e);
        }
    }
}

