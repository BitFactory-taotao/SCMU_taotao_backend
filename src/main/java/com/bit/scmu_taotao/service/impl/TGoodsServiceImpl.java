package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.scmu_taotao.dto.admin.GoodsAuditDetailDTO;
import com.bit.scmu_taotao.dto.admin.GoodsAuditListItemDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendGoodsDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendListResponseDTO;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.ChatMessageMapper;
import com.bit.scmu_taotao.mapper.TGoodsImageMapper;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.mapper.TGoodsMapper;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 35314
 * @description 针对表【t_goods(商品信息表)】的数据库操作Service实现
 * @createDate 2026-03-14 18:49:38
 */
@Slf4j
@Service
public class TGoodsServiceImpl extends ServiceImpl<TGoodsMapper, TGoods>
        implements TGoodsService {
    @Autowired
    private TGoodsImageMapper tGoodsImageMapper;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TBlacklistService tBlacklistService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private StompPushService stompPushService;

    @Autowired
    private ChatSessionService chatSessionService;

    private static final Set<String> VALID_TABS = Set.of(
            "recommend", "dormitory", "entertainment", "study", "pre-order"
    );

    private static final Map<String, String> TAB_TO_CATEGORY_NAME = Map.of(
            "dormitory", "宿舍用品",
            "entertainment", "娱乐用品",
            "study", "学习用品"
    );

    private static final Map<String, String> AUDIT_CATEGORY_NAME = Map.of(
            "dormitory", "宿舍用品",
            "entertainment", "娱乐用品",
            "study", "学习用品"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<String> getGoodsImageUrls(int goodsId) {
        // 根据商品ID查询图片列表
        List<TGoodsImage> images = tGoodsImageMapper.selectByGoodsId(goodsId);
        // 提取图片URL并返回
        return images.stream()
                .map(TGoodsImage::getImageUrl)
                .collect(Collectors.toList());
    }

    @Override
    public TGoods getGoodsById(int goodsId) {
        // 调用父类的getById方法获取商品信息
        return this.getById(goodsId);
    }

    @Override
    public Result getHomeGoodsList(String tab, String category, Integer page, Integer size, String currentUserId) {
        String effectiveTab = normalizeTab(tab, category);
        if (!VALID_TABS.contains(effectiveTab)) {
            return Result.fail(400, "tab参数非法");
        }

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        if ("recommend".equals(effectiveTab)) {
            return buildRecommendResult(currentUserId, safePage, safeSize);
        }

        return buildNormalTabResult(effectiveTab, safePage, safeSize, currentUserId);
    }

    @Override
    public Result searchHomeGoods(String keyword, Integer page, Integer size) {
        try {
            String trimmedKeyword = keyword == null ? "" : keyword.trim();
            if (trimmedKeyword.isEmpty()) {
                return Result.fail(400, "keyword不能为空");
            }

            int safePage = page == null || page < 1 ? 1 : page;
            int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);
            Set<String> blacklistedUserIds = getBlacklistedUserIds(UserContext.getUserId());

            LambdaQueryWrapper<TGoods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TGoods::getIsDelete, 0)
                    .eq(TGoods::getGoodsStatus, 0)
                    // 这里只搜索普通商品，不包含预售商品
                    .eq(TGoods::getGoodsType, 1)
                    .and(w -> w.like(TGoods::getGoodsName, trimmedKeyword)
                            .or()
                            .like(TGoods::getGoodsDesc, trimmedKeyword)
                            .or()
                            .like(TGoods::getGoodsNote, trimmedKeyword)
                            .or()
                            .like((TGoods::getUseScene), trimmedKeyword))
                    .orderByDesc(TGoods::getCreateTime);

            if (!blacklistedUserIds.isEmpty()) {
                queryWrapper.notIn(TGoods::getUserId, blacklistedUserIds);
            }

            Page<TGoods> goodsPage = this.page(new Page<>(safePage, safeSize), queryWrapper);

            List<Map<String, Object>> list = goodsPage.getRecords().stream().map(goods -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", goods.getGoodsId());
                row.put("name", goods.getGoodsName());
                row.put("remark", goods.getGoodsNote() == null ? "" : goods.getGoodsNote());
                row.put("price", goods.getPrice());
                row.put("imgUrl", getMainImage(goods.getGoodsId()));
                row.put("publishTime", goods.getCreateTime() == null ? "" : goods.getCreateTime().format(DATE_TIME_FORMATTER));

                TUser publisher = tUserService.getById(goods.getUserId());
                row.put("publisherName", publisher == null ? "未知" : publisher.getUserName());
                row.put("publisherId", goods.getUserId());
                return row;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", goodsPage.getTotal());
            data.put("pages", goodsPage.getPages());
            data.put("list", list);

            log.info("首页搜索完成：keyword={}, page={}, size={}, total={}", trimmedKeyword, safePage, safeSize, goodsPage.getTotal());
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("首页搜索失败：keyword={}, page={}, size={}", keyword, page, size, e);
            return Result.fail(500, "首页搜索失败，请稍后重试");
        }
    }

    @Override
    public Result getAuditGoodsList(Integer auditStatus, String category, String keyword, Integer page, Integer size) {
        try {
            int safeStatus = auditStatus == null ? 0 : auditStatus;
            if (safeStatus < 0 || safeStatus > 2) {
                return Result.fail(400, "auditStatus必须在0到2之间");
            }

            int safePage = page == null || page < 1 ? 1 : page;
            int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);
            String trimmedKeyword = keyword == null ? "" : keyword.trim();

            LambdaQueryWrapper<TGoods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TGoods::getIsDelete, 0);
            if (safeStatus == 0) {
                queryWrapper.and(w -> w.eq(TGoods::getIsAudited, 0).or().isNull(TGoods::getIsAudited));
            } else {
                queryWrapper.eq(TGoods::getIsAudited, safeStatus);
            }

            if (category != null && !category.isBlank()) {
                Integer categoryId = resolveAuditCategoryId(category.trim());
                if (categoryId == null) {
                    return Result.fail(400, "category参数非法");
                }
                queryWrapper.eq(TGoods::getCategoryId, categoryId);
            }

            if (!trimmedKeyword.isEmpty()) {
                queryWrapper.and(w -> w.like(TGoods::getGoodsName, trimmedKeyword)
                        .or()
                        .like(TGoods::getGoodsNote, trimmedKeyword)
                        .or()
                        .like(TGoods::getUserId, trimmedKeyword));
            }

            queryWrapper.orderByDesc(TGoods::getCreateTime);

            Page<TGoods> goodsPage = this.page(new Page<>(safePage, safeSize), queryWrapper);
            List<GoodsAuditListItemDTO> list = goodsPage.getRecords().stream()
                    .map(this::buildAuditGoodsListItem)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", goodsPage.getTotal());
            data.put("pages", goodsPage.getPages());
            data.put("list", list);

            log.info("商品巡检列表查询成功：auditStatus={}, category={}, keyword={}, page={}, size={}, total={}", safeStatus, category, trimmedKeyword, safePage, safeSize, goodsPage.getTotal());
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("商品巡检列表查询失败：auditStatus={}, category={}, keyword={}, page={}, size={}", auditStatus, category, keyword, page, size, e);
            return Result.fail("商品巡检列表查询失败，请稍后重试");
        }
    }

    @Override
    public Result getAuditGoodsDetail(Long goodsId) {
        try {
            if (goodsId == null || goodsId < 1) {
                return Result.fail(400, "goodsId必须大于0");
            }

            TGoods goods = this.getById(goodsId);
            if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
                return Result.fail(404, "商品不存在");
            }

            GoodsAuditDetailDTO detail = new GoodsAuditDetailDTO();
            detail.setId(goods.getGoodsId());
            detail.setName(goods.getGoodsName());
            detail.setDesc(goods.getGoodsDesc());
            detail.setPrice(goods.getPrice());
            detail.setPurpose(goods.getUseScene());
            detail.setExchangeAddr(goods.getExchangePlace());
            detail.setImgUrls(getGoodsImageUrls(goodsId.intValue()));
            detail.setPublishTime(goods.getCreateTime() == null ? null : goods.getCreateTime().format(DATE_TIME_FORMATTER));
            detail.setPublisher(tUserService.getPublisherInfo(goods.getUserId()));
            // goodsType: 1=出售, 2=预购；null 兜底为 sell
            detail.setType(goods.getGoodsType() != null && goods.getGoodsType() == 2 ? "buy" : "sell");

            log.info("商品巡检详情查询成功：goodsId={}", goodsId);
            return Result.ok("请求成功", detail);
        } catch (Exception e) {
            log.error("商品巡检详情查询失败：goodsId={}", goodsId, e);
            return Result.fail("商品巡检详情查询失败，请稍后重试");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result approveAuditGoods(List<Long> goodsIds) {
        try {
            List<Long> ids = normalizeGoodsIds(goodsIds);
            if (ids.isEmpty()) {
                return Result.fail(400, "goodsIds不能为空");
            }

            List<TGoods> goodsList = this.list(new LambdaQueryWrapper<TGoods>()
                    .in(TGoods::getGoodsId, ids)
                    .eq(TGoods::getIsDelete, 0));
            if (goodsList.isEmpty()) {
                return Result.fail(404, "商品不存在");
            }

            // 前置校验：只允许审核状态为"待巡检"（isAudited==0 或 null）的商品
            List<Long> invalidIds = goodsList.stream()
                    .filter(g -> g.getIsAudited() != null && g.getIsAudited() != 0)
                    .map(TGoods::getGoodsId)
                    .collect(Collectors.toList());
            if (!invalidIds.isEmpty()) {
                log.warn("商品巡检通过前置校验失败：以下商品不在待巡检状态 ids={}", invalidIds);
                return Result.fail(400, "以下商品不在待巡检状态，无法审核通过: " + invalidIds);
            }

            for (TGoods goods : goodsList) {
                goods.setIsAudited(1);
                goods.setGoodsStatus(0); // 审核通过后设为在线状态
                goods.setRejectReason(null);
                if (!this.updateById(goods)) {
                    throw new IllegalStateException("更新商品审核状态失败，goodsId=" + goods.getGoodsId());
                }
            }

            log.info("商品巡检通过成功：goodsIds={}, count={}", ids, goodsList.size());
            return Result.ok("操作成功", null);
        } catch (Exception e) {
            log.error("商品巡检通过失败：goodsIds={}", goodsIds, e);
            return Result.fail("商品巡检通过失败，请稍后重试");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result rejectAuditGoods(List<Long> goodsIds, String reason) {
        try {
            List<Long> ids = normalizeGoodsIds(goodsIds);
            String trimmedReason = reason == null ? "" : reason.trim();
            if (ids.isEmpty()) {
                return Result.fail(400, "goodsIds不能为空");
            }
            if (trimmedReason.isEmpty()) {
                return Result.fail(400, "reason不能为空");
            }

            List<TGoods> goodsList = this.list(new LambdaQueryWrapper<TGoods>()
                    .in(TGoods::getGoodsId, ids)
                    .eq(TGoods::getIsDelete, 0));
            if (goodsList.isEmpty()) {
                return Result.fail(404, "商品不存在");
            }

            // 前置校验：只允许审核状态为"待巡检"（isAudited==0 或 null）的商品
            List<Long> invalidIds2 = goodsList.stream()
                    .filter(g -> g.getIsAudited() != null && g.getIsAudited() != 0)
                    .map(TGoods::getGoodsId)
                    .collect(Collectors.toList());
            if (!invalidIds2.isEmpty()) {
                log.warn("商品巡检驳回前置校验失败：以下商品不在待巡检状态 ids={}", invalidIds2);
                return Result.fail(400, "以下商品不在待巡检状态，无法驳回: " + invalidIds2);
            }

            for (TGoods goods : goodsList) {
                goods.setGoodsStatus(2);
                goods.setIsAudited(2);
                goods.setRejectReason(trimmedReason);
                if (!this.updateById(goods)) {
                    throw new IllegalStateException("更新商品下架状态失败，goodsId=" + goods.getGoodsId());
                }

                if (goods.getUserId() != null && !goods.getUserId().isBlank()) {
                    String targetUserId = goods.getUserId();

                    // 1) 查找是否已有 system <-> user 的会话（复用）
                    ChatSession session = chatSessionService.findSessionByUsers("system", targetUserId);
                    Date now = new Date();
                    boolean createdSession = false;
                    if (session == null) {
                        // 2) 没有则创建新会话
                        session = new ChatSession();
                        session.setUser1Id("system");
                        session.setUser2Id(targetUserId);
                        session.setStatus(1);
                        session.setLastTime(now);
                        chatSessionService.save(session); // 保存后 session.chatId 应被填充
                        createdSession = true;
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
                    message.setMsgContent("您的商品【" + goods.getGoodsName() + "】因【" + trimmedReason + "】被管理员下架，如有异议请联系反馈。");
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
                    payload.put("goodsId", goods.getGoodsId());
                    payload.put("createTime", message.getCreateTime());
                    stompPushService.pushToUserQueue(targetUserId, "/queue/messages", payload);
                } else {
                    log.warn("商品巡检驳回时未找到有效发布者，跳过通知：goodsId={}", goods.getGoodsId());
                }
            }

            log.info("商品巡检驳回成功：goodsIds={}, count={}, reason={}", ids, goodsList.size(), trimmedReason);
            return Result.ok("操作成功，已下架相关商品并通知用户", null);
        } catch (Exception e) {
            log.error("商品巡检驳回失败：goodsIds={}, reason={}", goodsIds, reason, e);
            return Result.fail("商品巡检驳回失败，请稍后重试");
        }
    }

    private String normalizeTab(String tab, String category) {
        if (tab != null && !tab.isBlank()) {
            return tab.trim();
        }
        if (category != null && !category.isBlank()) {
            return category.trim();
        }
        return "recommend";
    }

    private Integer resolveAuditCategoryId(String category) {
        String categoryName = AUDIT_CATEGORY_NAME.get(category);
        if (categoryName == null) {
            return null;
        }
        QueryWrapper<TGoodsCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_name", categoryName).last("LIMIT 1");
        TGoodsCategory goodsCategory = tGoodsCategoryService.getOne(wrapper);
        return goodsCategory == null ? null : goodsCategory.getCategoryId();
    }

    private GoodsAuditListItemDTO buildAuditGoodsListItem(TGoods goods) {
        GoodsAuditListItemDTO item = new GoodsAuditListItemDTO();
        item.setId(goods.getGoodsId());
        item.setName(goods.getGoodsName());
        item.setRemark(goods.getGoodsNote() == null ? "" : goods.getGoodsNote());
        item.setPrice(goods.getPrice());
        item.setImgUrl(getMainImage(goods.getGoodsId()));
        item.setPublishTime(goods.getCreateTime() == null ? "" : goods.getCreateTime().format(DATE_TIME_FORMATTER));
        TUser publisher = tUserService.getById(goods.getUserId());
        item.setPublisherName(publisher == null ? "未知" : publisher.getUserName());
        item.setPublisherId(goods.getUserId());
        return item;
    }

    private List<Long> normalizeGoodsIds(List<Long> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            return Collections.emptyList();
        }
        return goodsIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Result buildRecommendResult(String currentUserId, int page, int size) {
        String userIdForRecommend = currentUserId == null ? "anonymous" : currentUserId;
        RecommendListResponseDTO recommendResult = recommendationService.getRecommendations(userIdForRecommend, page, size);

        List<Map<String, Object>> list = new ArrayList<>();
        if (recommendResult.getList() != null) {
            for (RecommendGoodsDTO item : recommendResult.getList()) {
                String publisherId = item.getPublisherInfo() == null ? null : item.getPublisherInfo().getUserId();
                Map<String, Object> row = new HashMap<>();
                row.put("id", item.getGoodsId());
                row.put("name", item.getGoodsName());
                row.put("remark", "");
                row.put("price", item.getPrice());
                row.put("imgUrl", item.getImageUrl() == null ? "" : item.getImageUrl());
                row.put("publishTime", item.getCreateTime());
                row.put("publisherName", item.getPublisherInfo() == null ? "未知" : item.getPublisherInfo().getUserName());
                row.put("publisherId", publisherId == null ? "" : publisherId);
                list.add(row);
            }
        }

        long total = recommendResult.getTotal() == null ? list.size() : recommendResult.getTotal();
        long pages = total == 0 ? 0 : (total + size - 1) / size;

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("pages", pages);
        data.put("list", list);

        return Result.ok("请求成功", data);
    }

    private Result buildNormalTabResult(String tab, int page, int size, String currentUserId) {
        Set<String> blacklistedUserIds = getBlacklistedUserIds(currentUserId);
        LambdaQueryWrapper<TGoods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TGoods::getIsDelete, 0)
                .eq(TGoods::getGoodsStatus, 0)
                .orderByDesc(TGoods::getCreateTime);

        if (!blacklistedUserIds.isEmpty()) {
            queryWrapper.notIn(TGoods::getUserId, blacklistedUserIds);
        }

        if ("pre-order".equals(tab)) {
            queryWrapper.eq(TGoods::getGoodsType, 2);
        } else {
            queryWrapper.eq(TGoods::getGoodsType, 1);
            Integer categoryId = resolveCategoryIdByTab(tab);
            if (categoryId == null) {
                return Result.fail(400, "tab参数非法");
            }
            queryWrapper.eq(TGoods::getCategoryId, categoryId);
        }

        Page<TGoods> goodsPage = this.page(new Page<>(page, size), queryWrapper);

        List<Map<String, Object>> list = goodsPage.getRecords().stream().map(goods -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", goods.getGoodsId());
            row.put("name", goods.getGoodsName());
            row.put("remark", goods.getGoodsNote() == null ? "" : goods.getGoodsNote());
            row.put("price", goods.getPrice());
            row.put("imgUrl", getMainImage(goods.getGoodsId()));
            row.put("publishTime", goods.getCreateTime() == null ? "" : goods.getCreateTime().format(DATE_TIME_FORMATTER));

            TUser publisher = tUserService.getById(goods.getUserId());
            row.put("publisherName", publisher == null ? "未知" : publisher.getUserName());
            row.put("publisherId", goods.getUserId());
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", goodsPage.getTotal());
        data.put("pages", goodsPage.getPages());
        data.put("list", list);

        return Result.ok("请求成功", data);
    }

    private Integer resolveCategoryIdByTab(String tab) {
        String categoryName = TAB_TO_CATEGORY_NAME.get(tab);
        if (categoryName == null) {
            return null;
        }
        QueryWrapper<TGoodsCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_name", categoryName).last("LIMIT 1");
        TGoodsCategory category = tGoodsCategoryService.getOne(wrapper);
        return category == null ? null : category.getCategoryId();
    }

    private String getMainImage(Long goodsId) {
        List<TGoodsImage> images = tGoodsImageMapper.selectByGoodsId(goodsId.intValue());
        if (images == null || images.isEmpty() || images.get(0).getImageUrl() == null) {
            return "";
        }
        return images.get(0).getImageUrl();
    }

    private Set<String> getBlacklistedUserIds(String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<TBlacklist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TBlacklist::getUserId, currentUserId)
                .eq(TBlacklist::getIsDelete, 0)
                .select(TBlacklist::getBlackUserId);
        List<TBlacklist> blacklist = tBlacklistService.list(queryWrapper);
        if (blacklist == null || blacklist.isEmpty()) {
            return Collections.emptySet();
        }
        return blacklist.stream()
                .map(TBlacklist::getBlackUserId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Result searchGoodsInternal(String keyword, Integer categoryId,
                                      BigDecimal minPrice, BigDecimal maxPrice,
                                      Integer page, Integer size) {
        try {
            int safePage = page == null || page < 1 ? 1 : page;
            int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);

            LambdaQueryWrapper<TGoods> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TGoods::getIsDelete, 0)
                    .eq(TGoods::getGoodsStatus, 0)
                    .eq(TGoods::getGoodsType, 1);

            // 关键词模糊搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                wrapper.and(w -> w.like(TGoods::getGoodsName, kw)
                        .or().like(TGoods::getGoodsDesc, kw)
                        .or().like(TGoods::getGoodsNote, kw)
                        .or().like(TGoods::getUseScene, kw));
            }

            // 分类过滤
            if (categoryId != null) {
                wrapper.eq(TGoods::getCategoryId, categoryId);
            }

            // 价格区间
            if (minPrice != null) {
                wrapper.ge(TGoods::getPrice, minPrice);
            }
            if (maxPrice != null) {
                wrapper.le(TGoods::getPrice, maxPrice);
            }

            wrapper.orderByDesc(TGoods::getCreateTime);

            Page<TGoods> goodsPage = this.page(new Page<>(safePage, safeSize), wrapper);

            List<Map<String, Object>> list = goodsPage.getRecords().stream().map(goods -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", goods.getGoodsId());
                row.put("name", goods.getGoodsName());
                row.put("remark", goods.getGoodsNote() == null ? "" : goods.getGoodsNote());
                row.put("price", goods.getPrice());
                row.put("imgUrl", getMainImage(goods.getGoodsId()));
                row.put("publishTime", goods.getCreateTime() == null ? "" : goods.getCreateTime().format(DATE_TIME_FORMATTER));

                TUser publisher = tUserService.getById(goods.getUserId());
                row.put("publisherName", publisher == null ? "未知" : publisher.getUserName());
                row.put("publisherId", goods.getUserId());
                return row;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", goodsPage.getTotal());
            data.put("pages", goodsPage.getPages());
            data.put("list", list);

            log.info("Internal 搜索完成：keyword={}, categoryId={}, minPrice={}, maxPrice={}, total={}",
                    keyword, categoryId, minPrice, maxPrice, goodsPage.getTotal());
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("Internal 搜索失败：keyword={}, categoryId={}", keyword, categoryId, e);
            return Result.fail(500, "搜索失败，请稍后重试");
        }
    }
}



