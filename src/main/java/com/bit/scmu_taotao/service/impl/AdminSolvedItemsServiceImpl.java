package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bit.scmu_taotao.dto.admin.*;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.TAccountAuditLogMapper;
import com.bit.scmu_taotao.mapper.TFeedbackMapper;
import com.bit.scmu_taotao.mapper.TGoodsMapper;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理员-解决事项总台服务实现
 */
@Slf4j
@Service
public class AdminSolvedItemsServiceImpl implements AdminSolvedItemsService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TGoodsImageService tGoodsImageService;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TFeedbackService tFeedbackService;

    @Autowired
    private TUserReportService tUserReportService;

    @Autowired
    private TAccountAuditLogMapper tAccountAuditLogMapper;

    @Autowired
    private TGoodsMapper tGoodsMapper;

    @Autowired
    private TFeedbackMapper tFeedbackMapper;

    @Override
    public Result getSolvedItemList(SolvedItemListRequest request) {
        try {
            if (request == null) {
                return Result.fail(400, "请求体不能为空");
            }

            String type = normalizeType(request.getType());
            String status = normalizeStatus(request.getStatus());
            String keyword = trimToNull(request.getKeyword());
            int pageNum = resolvePage(request.getPage());
            int pageSize = resolveSize(request.getSize());

            return switch (type) {
                case "goods" -> Result.ok("请求成功", buildGoodsList(status, keyword, pageNum, pageSize));
                case "feedback" -> Result.ok("请求成功", buildFeedbackList(keyword, pageNum, pageSize));
                case "user" -> Result.ok("请求成功", buildUserList(status, keyword, pageNum, pageSize));
                default -> Result.fail(400, "type不合法");
            };
        } catch (Exception e) {
            log.error("查询解决事项列表失败：type={}, status={}, keyword={}, error={}",
                    request == null ? null : request.getType(), request == null ? null : request.getStatus(),
                    request == null ? null : request.getKeyword(), e.getMessage(), e);
            return Result.fail("查询解决事项列表失败，请稍后重试");
        }
    }

    @Override
    public Result getSolvedItemDetail(String type, String id) {
        try {
            String solvedType = normalizeType(type);
            String solvedId = trimToNull(id);
            if (!StringUtils.hasText(solvedId)) {
                return Result.fail(400, "id不能为空");
            }

            return switch (solvedType) {
                case "goods" -> buildGoodsDetail(solvedId);
                case "feedback" -> buildFeedbackDetail(solvedId);
                case "user" -> buildUserDetail(solvedId);
                default -> Result.fail(400, "type不合法");
            };
        } catch (Exception e) {
            log.error("查询解决事项详情失败：type={}, id={}, error={}", type, id, e.getMessage(), e);
            return Result.fail("查询解决事项详情失败，请稍后重试");
        }
    }

    @Override
    public Result getSolvedItemCount() {
        try {
            long goodsCount = tGoodsService.count(new LambdaQueryWrapper<TGoods>()
                    .in(TGoods::getIsAudited, 1, 2)
                    .eq(TGoods::getIsDelete, 0));
            long feedbackCount = tFeedbackService.count(new LambdaQueryWrapper<TFeedback>()
                    .eq(TFeedback::getFeedbackStatus, 1)
                    .eq(TFeedback::getIsDelete, 0));
            long userCount = countLatestAuditUsers();

            SolvedItemCountDTO dto = new SolvedItemCountDTO();
            dto.setGoodsCount(goodsCount);
            dto.setFeedbackCount(feedbackCount);
            dto.setUserCount(userCount);
            dto.setTotalCount(goodsCount + feedbackCount + userCount);
            return Result.ok(dto);
        } catch (Exception e) {
            log.error("查询解决事项统计失败：{}", e.getMessage(), e);
            return Result.fail("查询解决事项统计失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result revokeSolvedItem(SolvedItemRevokeRequest request) {
        try {
            if (request == null) {
                return Result.fail(400, "请求体不能为空");
            }

            String type = normalizeType(request.getType());
            String id = trimToNull(request.getId());
            if (!StringUtils.hasText(id)) {
                return Result.fail(400, "id不能为空");
            }

            return switch (type) {
                case "goods" -> revokeGoods(id);
                case "feedback" -> revokeFeedback(id);
                case "user" -> revokeUser(id);
                default -> Result.fail(400, "type不合法");
            };
        } catch (Exception e) {
            log.error("撤销解决事项失败：type={}, id={}, error={}",
                    request == null ? null : request.getType(), request == null ? null : request.getId(), e.getMessage(), e);
            return Result.fail("撤销解决事项失败，请稍后重试");
        }
    }

    /**
     * 构建商品已解决列表
     */
    private Map<String, Object> buildGoodsList(String status, String keyword, int pageNum, int pageSize) {
        Page<TGoods> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TGoods::getIsAudited, 1, 2)
                .eq(TGoods::getIsDelete, 0);
        if (StringUtils.hasText(status)) {
            if ("APPROVED".equals(status)) {
                wrapper.eq(TGoods::getIsAudited, 1);
            } else if ("REJECTED".equals(status)) {
                wrapper.eq(TGoods::getIsAudited, 2);
            }
        }
        if (StringUtils.hasText(keyword)) {
            List<String> matchedUserIds = findMatchedUserIds(keyword);
            wrapper.and(w -> {
                w.like(TGoods::getGoodsName, keyword).or().like(TGoods::getGoodsNote, keyword);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in(TGoods::getUserId, matchedUserIds);
                }
            });
        }
        wrapper.orderByDesc(TGoods::getUpdateTime);

        Page<TGoods> resultPage = tGoodsService.page(page, wrapper);
        List<SolvedGoodsItemDTO> list = resultPage.getRecords().stream().map(goods -> {
            SolvedGoodsItemDTO dto = new SolvedGoodsItemDTO();
            dto.setGoodsId(goods.getGoodsId());
            dto.setName(goods.getGoodsName());
            dto.setRemark(goods.getGoodsNote());
            dto.setPrice(goods.getPrice());
            // TODO N+1查询解决
            dto.setImgUrl(resolveMainImage(goods.getGoodsId()));
            dto.setPublishTime(formatDateTime(goods.getCreateTime()));
            dto.setHandleStatus(resolveGoodsHandleStatus(goods.getIsAudited()));
            dto.setHandleTime(formatDateTime(goods.getUpdateTime()));
            return dto;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("total", resultPage.getTotal());
        data.put("pages", resultPage.getPages());
        data.put("list", list);
        return data;
    }

    /**
     * 构建反馈已解决列表
     */
    private Map<String, Object> buildFeedbackList(String keyword, int pageNum, int pageSize) {
        Page<TFeedback> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TFeedback::getFeedbackStatus, 1)
                .eq(TFeedback::getIsDelete, 0)
                .orderByDesc(TFeedback::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            List<String> matchedUserIds = findMatchedUserIds(keyword);
            wrapper.and(w -> {
                w.like(TFeedback::getFeedbackContent, keyword);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in(TFeedback::getUserId, matchedUserIds);
                }
            });
        }

        Page<TFeedback> resultPage = tFeedbackService.page(page, wrapper);
        Map<String, TUser> userMap = loadUserMap(resultPage.getRecords().stream()
                .map(TFeedback::getUserId)
                .filter(StringUtils::hasText)
                .toList());

        List<SolvedFeedbackItemDTO> list = resultPage.getRecords().stream().map(feedback -> {
            SolvedFeedbackItemDTO dto = new SolvedFeedbackItemDTO();
            dto.setFeedbackId(feedback.getFeedbackId());
            dto.setUserId(feedback.getUserId());
            TUser user = userMap.get(feedback.getUserId());
            if (user != null) {
                dto.setUserName(user.getUserName());
                dto.setAvatar(user.getAvatar());
            }
            dto.setReplyContent(feedback.getReplyContent());
            dto.setSubmitTime(formatDateTime(feedback.getCreateTime()));
            dto.setReplyTime(formatDateTime(feedback.getReplyTime()));
            return dto;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("total", resultPage.getTotal());
        data.put("pages", resultPage.getPages());
        data.put("list", list);
        return data;
    }

    /**
     * 构建账号审核已解决列表
     */
    private Map<String, Object> buildUserList(String status, String keyword, int pageNum, int pageSize) {
        // map status to action (db stored values)
        String action = null;
        if ("BAN".equals(status)) action = "ban";
        else if ("CLEAR".equals(status)) action = "clear";

        Page<TAccountAuditLog> page = new Page<>(pageNum, pageSize);
        IPage<TAccountAuditLog> result = tAccountAuditLogMapper.selectLatestPerUser(page, action, keyword);
        List<TAccountAuditLog> pageLogs = result.getRecords();

        List<String> userIds = pageLogs.stream()
                .map(TAccountAuditLog::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        Map<String, TUser> userMap = loadUserMap(userIds);

        List<SolvedUserItemDTO> list = pageLogs.stream().map(log -> {
            SolvedUserItemDTO dto = new SolvedUserItemDTO();
            dto.setUserId(log.getUserId());
            TUser user = userMap.get(log.getUserId());
            if (user != null) {
                dto.setUserName(user.getUserName());
                dto.setAvatar(user.getAvatar());
                dto.setRegisterTime(formatDateTime(user.getCreateTime()));
            }
            // TODO N+1查询解决
            dto.setReason(resolveMostFrequentReasonTag(log.getUserId()));
            dto.setHandleStatus(resolveAuditHandleStatus(log.getAction()));
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("list", list);
        return data;
    }

    /**
     * 商品详情
     */
    private Result buildGoodsDetail(String id) {
        Long goodsId = parseLongId(id);
        if (goodsId == null) {
            return Result.fail(400, "事项不存在或已被撤销");
        }

        TGoods goods = tGoodsMapper.selectById(goodsId);
        if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete()) || goods.getIsAudited() == null || goods.getIsAudited() == 0) {
            return Result.fail(400, "事项不存在或已被撤销");
        }

        TUser user = tUserService.getById(goods.getUserId());
        SolvedItemDetailResponse response = new SolvedItemDetailResponse();
        response.setType("goods");
        response.setHandleStatus(resolveGoodsHandleStatus(goods.getIsAudited()));
        response.setHandleTime(formatDateTime(goods.getUpdateTime()));

        SolvedGoodsDetailDTO detail = new SolvedGoodsDetailDTO();
        detail.setGoodsId(goods.getGoodsId());
        detail.setName(goods.getGoodsName());
        detail.setDesc(goods.getGoodsDesc());
        detail.setRemark(goods.getGoodsNote());
        detail.setPrice(goods.getPrice());
        detail.setPurpose(goods.getUseScene());
        detail.setExchangeAddr(goods.getExchangePlace());
        detail.setImgUrls(resolveGoodsImageUrls(goods.getGoodsId()));
        detail.setPublishTime(formatDateTime(goods.getCreateTime()));
        detail.setGoodsType(resolveGoodsType(goods.getGoodsType()));
        detail.setRejectReason(goods.getRejectReason());
        if (user != null) {
            SolvedGoodsDetailDTO.PublisherDTO publisher = new SolvedGoodsDetailDTO.PublisherDTO();
            publisher.setUserId(user.getUserId());
            publisher.setUserName(user.getUserName());
            publisher.setAvatar(user.getAvatar());
            publisher.setCreditScore(user.getCreditScore());
            publisher.setCreditStar(user.getCreditStar());
            detail.setPublisher(publisher);
        }
        response.setGoodsDetail(detail);
        return Result.ok("请求成功",response);
    }

    /**
     * 反馈详情
     */
    private Result buildFeedbackDetail(String id) {
        Long feedbackId = parseLongId(id);
        if (feedbackId == null) {
            return Result.fail(400, "事项不存在或已被撤销");
        }

        TFeedback feedback = tFeedbackMapper.selectById(feedbackId);
        if (feedback == null || Integer.valueOf(1).equals(feedback.getIsDelete()) || !Integer.valueOf(1).equals(feedback.getFeedbackStatus())) {
            return Result.fail(400, "事项不存在或已被撤销");
        }

        TUser user = tUserService.getById(feedback.getUserId());
        SolvedItemDetailResponse response = new SolvedItemDetailResponse();
        response.setType("feedback");
        response.setHandleStatus("PROCESSED");
        response.setHandleTime(formatDateTime(feedback.getReplyTime()));

        SolvedFeedbackDetailDTO detail = new SolvedFeedbackDetailDTO();
        detail.setFeedbackId(feedback.getFeedbackId());
        detail.setUserId(feedback.getUserId());
        if (user != null) {
            detail.setUserName(user.getUserName());
            detail.setAvatar(user.getAvatar());
        }
        detail.setSubmitTime(formatDateTime(feedback.getCreateTime()));
        detail.setContent(feedback.getFeedbackContent());
        detail.setReplyContent(feedback.getReplyContent());
        response.setFeedbackDetail(detail);
        return Result.ok("请求成功",response);
    }

    /**
     * 账号审核详情
     */
    private Result buildUserDetail(String id) {
        TAccountAuditLog auditLog = getLatestAuditLog(id);
        if (auditLog == null) {
            return Result.fail(400, "事项不存在或已被撤销");
        }

        TUser user = tUserService.getById(id);
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            return Result.fail(400, "事项不存在或已被撤销");
        }

        SolvedItemDetailResponse response = new SolvedItemDetailResponse();
        response.setType("user");
        response.setHandleStatus(resolveAuditHandleStatus(auditLog.getAction()));
        response.setHandleTime(formatDateTime(auditLog.getCreateTime()));

        SolvedUserDetailDTO detail = new SolvedUserDetailDTO();
        detail.setUserId(user.getUserId());
        detail.setUserName(user.getUserName());
        detail.setAvatar(user.getAvatar());
        detail.setCreditScore(user.getCreditScore());
        detail.setCreditStar(user.getCreditStar());
        detail.setReasonCategory(resolveMostFrequentReasonTag(id));
        detail.setViolationReason(auditLog.getReason());
        response.setUserDetail(detail);
        return Result.ok("请求成功",response);
    }

    /**
     * 撤销商品处理
     */
    private Result revokeGoods(String id) {
        Long goodsId = parseLongId(id);
        if (goodsId == null) {
            return Result.fail(400, "商品不存在或状态异常");
        }

        TGoods goods = tGoodsMapper.selectById(goodsId);
        if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete()) || goods.getIsAudited() == null
                || (goods.getIsAudited() != 1 && goods.getIsAudited() != 2)) {
            return Result.fail(400, "商品不存在或状态异常");
        }

        LambdaUpdateWrapper<TGoods> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TGoods::getGoodsId, goodsId)
                .in(TGoods::getIsAudited, 1, 2)
                .set(TGoods::getIsAudited, 0);
        if (Integer.valueOf(2).equals(goods.getIsAudited())) {
            updateWrapper.set(TGoods::getRejectReason, null);
        }

        boolean updated = tGoodsService.update(null, updateWrapper);
        if (!updated) {
            return Result.fail(400, "商品不存在或状态异常");
        }
        return Result.ok("撤销成功，该事项已重新回到待处理队列",null);
    }

    /**
     * 撤销反馈处理
     */
    private Result revokeFeedback(String id) {
        Long feedbackId = parseLongId(id);
        if (feedbackId == null) {
            return Result.fail(400, "反馈不存在或状态异常");
        }

        TFeedback feedback = tFeedbackMapper.selectById(feedbackId);
        if (feedback == null || Integer.valueOf(1).equals(feedback.getIsDelete()) || !Integer.valueOf(1).equals(feedback.getFeedbackStatus())) {
            return Result.fail(400, "反馈不存在或状态异常");
        }

        LambdaUpdateWrapper<TFeedback> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TFeedback::getFeedbackId, feedbackId)
                .eq(TFeedback::getFeedbackStatus, 1)
                .set(TFeedback::getFeedbackStatus, 0)
                .set(TFeedback::getReplyContent, null)
                .set(TFeedback::getReplyTime, null);

        boolean updated = tFeedbackService.update(null, updateWrapper);
        if (!updated) {
            return Result.fail(400, "反馈不存在或状态异常");
        }
        return Result.ok("撤销成功，该事项已重新回到待处理队列",null);
    }

    /**
     * 撤销账号审核处理
     */
    private Result revokeUser(String id) {
        TAccountAuditLog auditLog = getLatestAuditLog(id);
        if (auditLog == null) {
            return Result.fail(400, "账号审核记录不存在或已被撤销");
        }

        TUser user = tUserService.getById(id);
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            return Result.fail(400, "账号审核记录不存在或已被撤销");
        }

        LambdaUpdateWrapper<TUser> userUpdateWrapper = new LambdaUpdateWrapper<>();
        userUpdateWrapper.eq(TUser::getUserId, id)
                .set(TUser::getStatus, auditLog.getPreviousStatus());
        if (Integer.valueOf(0).equals(auditLog.getPreviousStatus())) {
            userUpdateWrapper.set(TUser::getViolationReason, null);
        }
        boolean userUpdated = tUserService.update(null, userUpdateWrapper);
        if (!userUpdated) {
            return Result.fail(400, "账号审核记录不存在或已被撤销");
        }

        auditLog.setIsDelete(1);
        boolean logUpdated = tAccountAuditLogMapper.updateById(auditLog) > 0;
        if (!logUpdated) {
            return Result.fail(400, "账号审核记录不存在或已被撤销");
        }

        return Result.ok("撤销成功，该事项已重新回到待处理队列",null);
    }

    /**
     * 查询与关键字匹配的用户ID列表
     */
    private List<String> findMatchedUserIds(String keyword) {
        List<TUser> users = tUserService.list(new LambdaQueryWrapper<TUser>()
                .eq(TUser::getIsDelete, 0)
                .and(w -> w.like(TUser::getUserName, keyword).or().like(TUser::getUserId, keyword)));
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(TUser::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 加载用户信息映射
     */
    private Map<String, TUser> loadUserMap(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TUser> users = tUserService.listByIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TUser::getUserId, u -> u, (a, b) -> a, HashMap::new));
    }

    /**
     * 统计账号审核去重后的数量
     */
    private long countLatestAuditUsers() {
        Page<TAccountAuditLog> page = new Page<>(1, 1);
        IPage<TAccountAuditLog> result = tAccountAuditLogMapper.selectLatestPerUser(page, null, null);
        return result == null ? 0L : result.getTotal();
    }

    /**
     * 查询最新账号审核记录
     */
    private TAccountAuditLog getLatestAuditLog(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return tAccountAuditLogMapper.selectOne(new LambdaQueryWrapper<TAccountAuditLog>()
                .eq(TAccountAuditLog::getUserId, userId)
                .eq(TAccountAuditLog::getIsDelete, 0)
                .orderByDesc(TAccountAuditLog::getId)
                .last("LIMIT 1"));
    }

    /**
     * 解析最常见的举报标签
     */
    private String resolveMostFrequentReasonTag(String userId) {
        if (!StringUtils.hasText(userId)) {
            return "";
        }
        QueryWrapper<TUserReport> wrapper = new QueryWrapper<>();
        wrapper.select("tag", "COUNT(*) AS cnt")
                .eq("target_id", userId)
                .eq("status", 1)
                .groupBy("tag")
                .orderByDesc("cnt")
                .last("LIMIT 1");
        List<Map<String, Object>> rows = tUserReportService.listMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        Object tag = rows.get(0).get("tag");
        return tag == null ? "" : String.valueOf(tag);
    }

    /**
     * 获取商品主图
     */
    private String resolveMainImage(Long goodsId) {
        List<String> imageUrls = resolveGoodsImageUrls(goodsId);
        if (imageUrls.isEmpty()) {
            return "";
        }
        return imageUrls.get(0);
    }

    /**
     * 获取商品图片列表
     */
    private List<String> resolveGoodsImageUrls(Long goodsId) {
        if (goodsId == null) {
            return Collections.emptyList();
        }
        List<String> imageUrls = tGoodsImageService.getGoodsImageUrls(goodsId);
        return imageUrls == null ? Collections.emptyList() : imageUrls;
    }

    /**
     * 解析商品状态文案
     */
    private String resolveGoodsHandleStatus(Integer isAudited) {
        if (Integer.valueOf(1).equals(isAudited)) {
            return "APPROVED";
        }
        if (Integer.valueOf(2).equals(isAudited)) {
            return "REJECTED";
        }
        return "";
    }

    /**
     * 解析账号处理状态文案
     */
    private String resolveAuditHandleStatus(String action) {
        if ("ban".equalsIgnoreCase(action)) {
            return "BAN";
        }
        if ("clear".equalsIgnoreCase(action)) {
            return "CLEAR";
        }
        return "";
    }

    /**
     * 解析商品类型文案
     */
    private String resolveGoodsType(Integer goodsType) {
        if (goodsType == null) {
            return "";
        }
        if (goodsType == 0) {
            return "sell";
        }
        if (goodsType == 1) {
            return "buy";
        }
        return String.valueOf(goodsType);
    }

    /**
     * 格式化时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 解析字符串ID
     */
    private Long parseLongId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 规范化类型
     */
    private String normalizeType(String type) {
        String value = trimToNull(type);
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化状态
     */
    private String normalizeStatus(String status) {
        String value = trimToNull(status);
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    /**
     * 解析页码
     */
    private int resolvePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    /**
     * 解析分页大小
     */
    private int resolveSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    /**
     * 去除首尾空白并在空字符串时返回 null
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
