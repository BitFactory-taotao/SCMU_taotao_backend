package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.UserReportRequest;
import com.bit.scmu_taotao.dto.admin.AdminReportDetailDTO;
import com.bit.scmu_taotao.dto.admin.AdminReportListItemDTO;
import com.bit.scmu_taotao.dto.admin.AdminReportPageRequest;
import com.bit.scmu_taotao.dto.admin.AdminReportVerifyRequest;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.mapper.TUserReportMapper;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 35314
 * @description 针对表【t_user_report(用户举报记录表)】的数据库操作Service实现
 * @createDate 2026-05-20 18:48:56
 */
@Slf4j
@Service
public class TUserReportServiceImpl extends ServiceImpl<TUserReportMapper, TUserReport>
        implements TUserReportService {

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TCreditLogService creditLogService;

    @Autowired
    private StompPushService stompPushService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    // Tag 中文映射静态常量
    private static final Map<String, String> TAG_DESC_MAP = Map.of(
            "LOW_CREDIT", "信用不良",
            "GOODS_VIOLATION", "商品违规",
            "LANG_VIOLATION", "语言违规",
            "OTHER", "其他"
    );

    // 日期格式化
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result reportUser(String targetId, UserReportRequest request) {
        String reporterId = UserContext.getUserId();
        if (reporterId == null || reporterId.trim().isEmpty()) {
            return Result.fail(401, "用户未登录或登录已过期");
        }
        if (targetId == null || targetId.trim().isEmpty()) {
            return Result.fail(400, "被举报用户ID不能为空");
        }
        if (reporterId.equals(targetId)) {
            return Result.fail(400, "不能举报自己");
        }

        TUser targetUser = tUserService.getById(targetId);
        if (targetUser == null) {
            return Result.fail(404, "被举报用户不存在");
        }

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long count = this.count(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, reporterId)
                .eq(TUserReport::getTargetId, targetId)
                .ge(TUserReport::getCreateTime, twentyFourHoursAgo));
        if (count > 0) {
            return Result.fail(400, "24小时内不能重复举报同一用户");
        }

        TUserReport report = new TUserReport();
        report.setReporterId(reporterId);
        report.setTargetId(targetId);
        report.setTag(request.getCategory());
        report.setContent(request.getContent());
        List<String> imgUrls = request.getImgUrls();
        List<String> validUrls = imgUrls == null ? List.of() : imgUrls.stream()
                .filter(url -> url != null && !url.trim().isEmpty())
                .collect(Collectors.toList());
        report.setImgUrls(validUrls.isEmpty() ? null : String.join(",", validUrls));
        report.setStatus(0);

        boolean saved = this.save(report);
        if (!saved) {
            log.warn("举报提交失败：reporterId={}, targetId={}", reporterId, targetId);
            return Result.fail(500, "举报提交失败");
        }
        return Result.ok("举报提交成功");
    }

    @Override
    public Result getAdminReportList(AdminReportPageRequest request) {
        // 构建目标用户 ID 列表（如果有关键字搜索）
        List<String> targetIds = null;
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            LambdaQueryWrapper<TUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.and(w -> w.like(TUser::getUserName, request.getKeyword())
                    .or().like(TUser::getUserId, request.getKeyword()));
            List<TUser> users = tUserService.list(userWrapper);
            if (users.isEmpty()) {
                return Result.ok(List.of(), 0L);
            }
            targetIds = users.stream().map(TUser::getUserId).collect(Collectors.toList());
        }

        // 构建举报查询条件
        LambdaQueryWrapper<TUserReport> wrapper = new LambdaQueryWrapper<>();
        if (targetIds != null) {
            wrapper.in(TUserReport::getTargetId, targetIds);
        }
        wrapper.eq(TUserReport::getStatus, 0);
        wrapper.orderByDesc(TUserReport::getCreateTime);

        // 分页查询
        Page<TUserReport> page = this.page(new Page<>(request.getPage(), request.getSize()), wrapper);

        // 批量查用户信息
        Set<String> distinctTargetIds = page.getRecords().stream()
                .map(TUserReport::getTargetId)
                .collect(Collectors.toSet());
        Map<String, TUser> userMap = new HashMap<>();
        if (!distinctTargetIds.isEmpty()) {
            List<TUser> users = tUserService.listByIds(distinctTargetIds);
            users.forEach(u -> userMap.put(u.getUserId(), u));
        }

        // 组装响应
        List<AdminReportListItemDTO> dtoList = page.getRecords().stream().map(report -> {
            AdminReportListItemDTO dto = new AdminReportListItemDTO();
            dto.setReportId(report.getId());

            TUser targetUser = userMap.get(report.getTargetId());
            if (targetUser != null) {
                AdminReportListItemDTO.TargetUserVO vo = new AdminReportListItemDTO.TargetUserVO();
                vo.setId(targetUser.getUserId());
                vo.setName(targetUser.getUserName());
                vo.setAvatar(targetUser.getAvatar());
                dto.setTargetUser(vo);
            }

            dto.setTag(report.getTag());
            dto.setTagDesc(TAG_DESC_MAP.getOrDefault(report.getTag(), "未知"));
            dto.setCreateTime(report.getCreateTime().format(DATETIME_FORMATTER));
            dto.setStatus(report.getStatus());
            return dto;
        }).collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    @Override
    public Result getReportDetail(Long reportId) {
        TUserReport report = this.getById(reportId);
        if (report == null) {
            return Result.fail(404, "举报记录不存在");
        }

        TUser targetUser = tUserService.getById(report.getTargetId());

        AdminReportDetailDTO dto = new AdminReportDetailDTO();
        dto.setReportId(report.getId());

        if (targetUser != null) {
            AdminReportListItemDTO.TargetUserVO vo = new AdminReportListItemDTO.TargetUserVO();
            vo.setId(targetUser.getUserId());
            vo.setName(targetUser.getUserName());
            vo.setAvatar(targetUser.getAvatar());
            dto.setTargetUser(vo);
        }

        dto.setTag(report.getTag());
        dto.setTagDesc(TAG_DESC_MAP.getOrDefault(report.getTag(), "未知"));
        dto.setContent(report.getContent());

        // 解析图片URL列表
        List<String> imgUrls = new ArrayList<>();
        if (report.getImgUrls() != null && !report.getImgUrls().trim().isEmpty()) {
            imgUrls = Arrays.stream(report.getImgUrls().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        dto.setImgUrls(imgUrls);

        dto.setCreateTime(report.getCreateTime().format(DATETIME_FORMATTER));

        return Result.ok(dto);
    }

    @Override
    @Transactional
    public Result verifyReport(Long reportId, AdminReportVerifyRequest request) {
        TUserReport report = this.getById(reportId);
        if (report == null) {
            return Result.fail(404, "举报记录不存在");
        }

        // 校验举报状态（只能处理待审核的举报）
        if (report.getStatus() != null && report.getStatus() != 0) {
            return Result.fail(400, "该举报已处理，请勿重复操作");
        }

        if ("PASS".equals(request.getAction())) {
            // ===== 属实处理 =====
            if (request.getDeductScore() == null || request.getDeductScore() <= 0) {
                return Result.fail(400, "扣分值不能为空且须大于0");
            }

            // 获取被举报用户
            TUser targetUser = tUserService.getById(report.getTargetId());
            if (targetUser == null) {
                return Result.fail(404, "被举报用户不存在");
            }

            // 计算扣分后的新分数（下限为0）
            int newScore = Math.max(0, targetUser.getCreditScore() - request.getDeductScore());
            targetUser.setCreditScore(newScore);
            tUserService.updateById(targetUser);

            // 记录信用流水
            TCreditLog creditLog = new TCreditLog();
            creditLog.setUserId(report.getTargetId());
            creditLog.setScoreChange(-request.getDeductScore());
            creditLog.setChangeType(report.getTag());
            creditLog.setReason("举报属实：" + TAG_DESC_MAP.get(report.getTag()) +
                    "，扣除" + request.getDeductScore() + "分");
            creditLogService.save(creditLog);

            // 更新举报状态
            report.setStatus(1);
            this.updateById(report);

            // --- 通知举报人：举报审核结果 ---
            sendSystemMessage(report.getReporterId(),
                    "您提交的举报已审核处理完毕，结果：属实，已对被举报人扣除" + request.getDeductScore() + "分。");

            // --- 通知被举报人：信用分被扣 ---
            sendSystemMessage(report.getTargetId(),
                    "您因【" + TAG_DESC_MAP.get(report.getTag()) + "】被举报属实，信誉分扣除"
                            + request.getDeductScore() + "分，当前信誉分" + newScore + "。");
            // 组装返回数据
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("reportId", reportId);
            data.put("targetUserId", report.getTargetId());
            data.put("newCreditScore", newScore);
            data.put("status", "PROCESSED");
            return Result.ok("审核处理成功", data);

        } else if ("REJECT".equals(request.getAction())) {
            // ===== 驳回处理 =====
            TUser targetUser = tUserService.getById(report.getTargetId());
            Integer originalScore = targetUser != null ? targetUser.getCreditScore() : null;

            // 更新举报状态
            report.setStatus(1);
            this.updateById(report);

            // --- 通知举报人：举报驳回 ---
            sendSystemMessage(report.getReporterId(),
                    "您提交的举报已审核处理完毕，结果：不属实。");

            // 组装返回数据
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("reportId", reportId);
            data.put("targetUserId", report.getTargetId());
            data.put("newCreditScore", originalScore);
            data.put("status", "PROCESSED");
            return Result.ok("审核处理成功", data);

        } else {
            return Result.fail(400, "未知的处理操作类型");
        }

    }
    /**
     * 发送系统通知消息（复用商品审核/反馈处理的模式）
     * 1. 查找/创建 system↔user 会话
     * 2. 保存 chat_message（msgType=0, sendId="system"）
     * 3. 更新会话 lastMsg/lastTime
     * 4. STOMP 推送到 /queue/messages
     */
    private void sendSystemMessage(String targetUserId, String content) {
        try {
            // 1) 查找是否已有 system↔user 的会话
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

            // 2) 保存系统消息
            ChatMessage message = new ChatMessage();
            message.setChatId(session.getChatId());
            message.setSendId("system");
            message.setReceiveId(targetUserId);
            message.setMsgType(0);
            message.setMsgContent(content);
            message.setIsRead(0);
            message.setIsDelete(0);
            chatMessageMapper.insert(message);

            // 3) 更新会话 lastMsg/lastTime
            ChatSession updateSession = new ChatSession();
            updateSession.setChatId(session.getChatId());
            updateSession.setLastMsg(content);
            updateSession.setLastTime(now);
            chatSessionService.updateById(updateSession);

            // 4) STOMP 推送
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



