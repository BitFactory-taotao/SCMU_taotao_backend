package com.bit.scmu_taotao.controller.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.scmu_taotao.config.storage.S3StorageProperties;
import com.bit.scmu_taotao.dto.agent.*;
import com.bit.scmu_taotao.dto.goods.GoodsResponseDTO;
import com.bit.scmu_taotao.exception.SensitiveWordException;
import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import com.bit.scmu_taotao.dto.recommend.RecommendListResponseDTO;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.util.storage.ObjectKeyParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Internal API Controller
 * <p>
 * 供 Campus Trading Agent（Python 服务）通过 localhost 调用。
 * 不走 LoginInterceptor，使用 X-Internal-Secret 请求头校验。
 * userId 由 Agent 显式传入（Agent 侧已验证用户 Token）。
 */
@Slf4j
@RestController
@RequestMapping("/internal")
public class AgentInternalController {

    @Value("${app.internal-api.secret}")
    private String internalSecret;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private TGoodsImageService tGoodsImageService;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TTradeService tTradeService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private S3StorageProperties s3StorageProperties;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 2.2 Token 校验 ====================

    /**
     * 验证用户 Token 并返回 userId 和角色
     */
    @PostMapping("/auth/verify")
    public Result verifyToken(HttpServletRequest request, @RequestBody TokenVerifyRequest body) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        String token = body.getToken();
        if (token == null || token.isBlank()) {
            return Result.fail(400, "token不能为空");
        }

        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String rawValue = tokenUtil.validateToken(cleanToken);
        if (rawValue == null) {
            return Result.fail(401, "Token无效或已过期");
        }

        rawValue = rawValue.replace("\"", "");
        boolean isAdmin = rawValue.startsWith("ADMIN:");
        String userId = isAdmin ? rawValue.replace("ADMIN:", "") : rawValue;
        String userRole = isAdmin ? "ADMIN" : "USER";

        log.info("Internal Token 校验成功：userId={}, role={}", userId, userRole);
        return Result.ok(new TokenVerifyResponse(userId, userRole));
    }

    // ==================== 2.3 商品增强搜索 ====================

    /**
     * 增强商品搜索：支持 keyword + 分类 + 价格区间
     */
    @GetMapping("/goods/search")
    public Result searchGoods(HttpServletRequest request, InternalGoodsSearchRequest query) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        return tGoodsService.searchGoodsInternal(
                query.getKeyword(), query.getCategoryId(),
                query.getMinPrice(), query.getMaxPrice(),
                query.getPage(), query.getSize());
    }

    // ==================== 2.4 商品详情 ====================

    /**
     * 获取商品完整详情（含图片 URL、发布者信息）
     */
    @GetMapping("/goods/{goodsId}")
    public Result getGoodsDetail(HttpServletRequest request, @PathVariable Long goodsId) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        if (goodsId == null || goodsId < 1) {
            return Result.fail(400, "goodsId必须大于0");
        }

        TGoods goods = tGoodsService.getGoodsById(goodsId.intValue());
        if (goods == null || Integer.valueOf(1).equals(goods.getIsDelete())) {
            return Result.fail(404, "商品不存在");
        }

        GoodsResponseDTO data = new GoodsResponseDTO();
        data.setId(goods.getGoodsId());
        data.setName(goods.getGoodsName());
        data.setDesc(goods.getGoodsDesc());
        data.setPrice(goods.getPrice());
        data.setPurpose(goods.getUseScene());
        data.setExchangeAddr(goods.getExchangePlace());
        data.setPublishTime(goods.getCreateTime() != null ? goods.getCreateTime().format(DTF) : "");
        data.setType(goods.getGoodsType() != null && goods.getGoodsType() == 2 ? "buy" : "sell");
        data.setImgUrls(tGoodsService.getGoodsImageUrls(goodsId.intValue()));

        PublisherDTO publisher = tUserService.getPublisherInfo(goods.getUserId());
        data.setPublisher(publisher);

        return Result.ok(data);
    }

    // ==================== 2.5 发布商品 ====================

    /**
     * 发布商品（Agent 代理用户发布，userId 显式传入）
     */
    @PostMapping("/goods")
    public Result publishGoods(HttpServletRequest request, @RequestBody InternalGoodsPublishRequest body) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        String userId = body.getUserId();
        if (userId == null || userId.isBlank()) {
            return Result.fail(400, "userId不能为空");
        }

        String name = body.getName();

        // 校验用户是否存在
        TUser user = tUserService.getById(userId);
        if (user == null) {
            return Result.fail(400, "用户不存在");
        }
        String desc = body.getDesc();
        String remark = body.getRemark();
        BigDecimal price = body.getPrice();
        String purpose = body.getPurpose();
        String exchangeAddr = body.getExchangeAddr();
        List<String> imgUrls = body.getImgUrls();
        String type = body.getType();
        String categoryName = body.getCategoryName();
        String draftId = body.getDraftId();

        // 参数校验
        if (name == null || name.isBlank()) return Result.fail(400, "商品名称不能为空");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) return Result.fail(400, "价格必须大于0");
        if (type == null || (!"sell".equals(type) && !"buy".equals(type))) return Result.fail(400, "type必须为sell或buy");
        if (categoryName == null || categoryName.isBlank()) return Result.fail(400, "分类不能为空");

        // 查询商品分类
        QueryWrapper<TGoodsCategory> categoryWrapper = new QueryWrapper<>();
        categoryWrapper.eq("category_name", categoryName);
        TGoodsCategory goodsCategory = tGoodsCategoryService.getOne(categoryWrapper);
        if (goodsCategory == null) {
            return Result.fail(400, "商品分类不存在: " + categoryName);
        }

        // 敏感词校验
        try {
            sensitiveWordService.validateGoods(
                    name != null ? name : "",
                    desc != null ? desc : "",
                    remark != null ? remark : "",
                    purpose != null ? purpose : "",
                    exchangeAddr != null ? exchangeAddr : "");
        } catch (SensitiveWordException e) {
            return Result.fail(400, "商品信息包含敏感词: " + e.getMessage());
        }

        // 创建商品实体
        TGoods goods = new TGoods();
        goods.setUserId(userId);
        goods.setCategoryId(goodsCategory.getCategoryId());
        goods.setGoodsName(name);
        goods.setGoodsDesc(desc);
        goods.setGoodsNote(remark);
        goods.setPrice(price);
        goods.setUseScene(purpose);
        goods.setExchangePlace(exchangeAddr);
        goods.setGoodsType("sell".equals(type) ? 1 : 2);
        goods.setGoodsStatus(0);
        goods.setIsAudited(0);
        goods.setIsDelete(0);

        tGoodsService.save(goods);

        // 保存商品图片
        if (imgUrls != null && !imgUrls.isEmpty()) {
            int sort = 1;
            for (String imgUrl : imgUrls) {
                if (imgUrl == null || imgUrl.isBlank()) continue;
                String objectKey = ObjectKeyParser.extractObjectKey(imgUrl, s3StorageProperties.getBucket());
                if (objectKey == null || objectKey.isBlank()) continue;
                TGoodsImage goodsImage = new TGoodsImage();
                goodsImage.setGoodsId(goods.getGoodsId());
                goodsImage.setImageUrl(imgUrl);
                goodsImage.setSort(sort++);
                tGoodsImageService.save(goodsImage);
            }
        }

        // 删除对应草稿
        if (draftId != null && !draftId.isEmpty()) {
            String draftKey = "goods_draft:" + userId + ":" + draftId;
            String draftIdsKey = "goods_draft:" + userId + ":ids";
            redisService.removeFromSet(draftIdsKey, draftId);
            redisService.delete(draftKey);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("goodsId", goods.getGoodsId());
        data.put("imgUrls", imgUrls);

        log.info("Internal 发布商品成功：userId={}, goodsId={}, name={}", userId, goods.getGoodsId(), name);
        return Result.ok("发布成功", data);
    }

    // ==================== 2.6 分类列表 ====================

    /**
     * 获取所有商品分类
     */
    @GetMapping("/categories")
    public Result getCategories(HttpServletRequest request) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        List<TGoodsCategory> categories = tGoodsCategoryService.list();
        List<InternalCategoryResponse> data = categories.stream()
                .map(c -> new InternalCategoryResponse(c.getCategoryId(), c.getCategoryName(), c.getSort()))
                .collect(Collectors.toList());

        return Result.ok(data);
    }

    // ==================== 2.7 推荐 ====================

    /**
     * 获取推荐商品列表（个性化/冷启动）
     */
    @GetMapping("/recommend")
    public Result getRecommendations(HttpServletRequest request,
                                     @RequestParam String userId,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        int safeSize = Math.min(size, 50);
        RecommendListResponseDTO result = recommendationService.getRecommendations(userId, page, safeSize);
        return Result.ok(result);
    }

    // ==================== 2.8 用户数据 ====================

    /**
     * 获取用户发布的商品（含出售和预购）
     */
    @GetMapping("/user/goods")
    public Result getUserGoods(HttpServletRequest request,
                               @RequestParam String userId,
                               @RequestParam(defaultValue = "0") String goodsStatus,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        UserContext.setUserId(userId);
        try {
            return tUserService.getSellGoods(page, size, goodsStatus);
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 获取用户收藏
     */
    @GetMapping("/user/favorites")
    public Result getUserFavorites(HttpServletRequest request,
                                   @RequestParam String userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        UserContext.setUserId(userId);
        try {
            return tUserService.getFavorites(page, size);
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 获取用户交易记录（作为买家或卖家）
     */
    @GetMapping("/user/trades")
    public Result getUserTrades(HttpServletRequest request,
                                @RequestParam String userId,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        int safePage = page < 1 ? 1 : page;
        int safeSize = Math.min(size, 50);

        // 查询用户作为买家或卖家的交易记录
        LambdaQueryWrapper<TTrade> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(TTrade::getBuyerId, userId).or().eq(TTrade::getSellerId, userId))
                .orderByDesc(TTrade::getCreateTime);

        Page<TTrade> pageResult = tTradeService.page(new Page<>(safePage, safeSize), wrapper);
        List<TTrade> trades = pageResult.getRecords();

        if (trades.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("total", 0L);
            data.put("list", Collections.emptyList());
            return Result.ok(data);
        }

        // 批量查询关联商品
        List<Long> goodsIds = trades.stream().map(TTrade::getGoodsId).distinct().collect(Collectors.toList());
        // 过滤已删除和已下架的商品
        List<TGoods> goodsList = tGoodsService.lambdaQuery()
                .in(TGoods::getGoodsId, goodsIds)
                .eq(TGoods::getIsDelete, 0)
                .ne(TGoods::getGoodsStatus, 2)
                .list();
        Map<Long, TGoods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(TGoods::getGoodsId, g -> g));

        // 批量查询交易对方
        Set<String> counterpartyIds = new HashSet<>();
        for (TTrade t : trades) {
            if (userId.equals(t.getBuyerId())) {
                counterpartyIds.add(t.getSellerId());
            } else {
                counterpartyIds.add(t.getBuyerId());
            }
        }
        Map<String, TUser> userMap = tUserService.listByIds(new ArrayList<>(counterpartyIds)).stream()
                .collect(Collectors.toMap(TUser::getUserId, u -> u));

        List<Map<String, Object>> list = new ArrayList<>();
        for (TTrade t : trades) {
            Map<String, Object> item = new HashMap<>();
            item.put("tradeId", t.getTradeId());
            item.put("goodsId", t.getGoodsId());
            item.put("tradePrice", t.getTradePrice());
            item.put("tradePlace", t.getTradePlace());
            item.put("tradeTime", t.getTradeTime() != null ? t.getTradeTime().format(DTF) : null);

            boolean isBuyer = userId.equals(t.getBuyerId());
            item.put("role", isBuyer ? "buyer" : "seller");

            String counterpartyId = isBuyer ? t.getSellerId() : t.getBuyerId();
            item.put("counterpartyId", counterpartyId);
            TUser counterparty = userMap.get(counterpartyId);
            item.put("counterpartyName", counterparty != null ? counterparty.getUserName() : "未知");

            TGoods goods = goodsMap.get(t.getGoodsId());
            if (goods != null) {
                item.put("goodsName", goods.getGoodsName());
                item.put("imgUrl", tGoodsService.getGoodsImageUrls(goods.getGoodsId().intValue())
                        .stream().findFirst().orElse(""));
            }
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotal());
        data.put("list", list);
        return Result.ok(data);
    }

    /**
     * 获取用户基本信息
     */
    @GetMapping("/user/info")
    public Result getUserInfo(HttpServletRequest request, @RequestParam String userId) {
        Result auth = validateInternalSecret(request);
        if (auth != null) return auth;

        TUser user = tUserService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("userName", user.getUserName());
        data.put("avatar", user.getAvatar());
        data.put("creditScore", user.getCreditScore());
        data.put("creditStar", user.getCreditStar());
        data.put("status", user.getStatus());
        return Result.ok(data);
    }

    // ==================== 内部方法 ====================

    /**
     * 校验 X-Internal-Secret 请求头
     *
     * @return null 表示校验通过，否则返回错误 Result
     */
    private Result validateInternalSecret(HttpServletRequest request) {
        String secret = request.getHeader("X-Internal-Secret");
        if (secret == null || !secret.equals(internalSecret)) {
            log.warn("Internal API 鉴权失败：secret 不匹配，来源 IP={}", request.getRemoteAddr());
            return Result.fail(403, "Forbidden");
        }
        return null;
    }
}
