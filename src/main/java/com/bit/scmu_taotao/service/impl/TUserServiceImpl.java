package com.bit.scmu_taotao.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.client.HttpResponseHandler;
import com.bit.scmu_taotao.client.HttpResponseHandlerImpl;
import com.bit.scmu_taotao.client.HttpResponseResult;
import com.bit.scmu_taotao.client.thread.WebVpnLoginThread;
import com.bit.scmu_taotao.dto.GoodsEditRequest;
import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.mapper.TUserMapper;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.KeyDescription;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 35314
 * @description 针对表【t_user(用户基础信息表)】的数据库操作 Service 实现
 * @createDate 2026-03-12 18:35:11
 */
@Slf4j
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser>
        implements TUserService{

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private TFavoriteService favoriteService;

    @Autowired
    private TGoodsService goodsService;

    @Autowired
    private TGoodsImageService goodsImageService;
    @Autowired
    private TTradeService tradeService;
    @Autowired
    private TEvaluateService evaluateService;

    // WebVPN 地址（从配置文件读取或默认值）
    private static final String WEBVPN_URL = "https://webvpn.scuec.edu.cn/";

    private final HttpResponseHandler httpResponseHandler = new HttpResponseHandlerImpl();

    private static final Map<String, Integer> STATUS_STR_TO_INT = Map.of(
            "online", 0, "sold", 1, "offline", 2, "audit", 3
    );
    private static final Map<Integer, String> STATUS_INT_TO_STR = Map.of(
            0, "online", 1, "sold", 2, "offline", 3, "audit"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result login(String userId, String password) {
        try {
            log.info("用户登录：userId={}", userId);

            // 1. 通过 WebVPN 进行身份验证
            CookieStore cookieStore = new BasicCookieStore();
            WebVpnLoginThread loginThread = new WebVpnLoginThread(
                    userId, password, cookieStore, WEBVPN_URL, redisService
            );

            // 执行同步登录
            Result vpnLoginResult = loginThread.syncLogin();
            if (vpnLoginResult.getCode() != 200) {
                return Result.fail("登录失败");
            }

            // 2. 登录成功后，获取真实用户名
            String realUserName = (String) vpnLoginResult.getData();
            log.info("webvpn 登录成功，真实用户名：{}", realUserName);
            // 3. 查询或创建用户记录
            TUser user = this.getById(userId);
            if (user == null) {
                // 首次登录，创建新用户
                user = new TUser();
                user.setUserId(userId);
                user.setUserName(realUserName != null ? realUserName : userId);
                user.setCreditScore(100); // 初始信誉分 100
                user.setCreditStar(new BigDecimal("5.0")); // 初始信誉星级 5.0
                user.setCreateTime(java.time.LocalDateTime.now()); // 设置创建时间
                user.setUpdateTime(java.time.LocalDateTime.now()); // 设置更新时间
                user.setIsDelete(0); // 未删除
                this.save(user);
                log.info("新用户注册：userId={}, userName={}", userId, user.getUserName());
            } else {
                // 更新用户名（如果发生变化）
                if (realUserName != null && !realUserName.equals(user.getUserName())) {
                    user.setUserName(realUserName);
                    user.setUpdateTime(java.time.LocalDateTime.now()); // 更新更新时间
                    this.updateById(user);
                }
            }

            // 4. 序列化 Cookie 并存储到 Redis
            String cookieKey = KeyDescription.SSFW + userId;
            try {
                byte[] serializedCookie = CookieSerialization.serialize(cookieStore);
                redisService.setWithExpire(cookieKey, serializedCookie, 2, java.util.concurrent.TimeUnit.HOURS);
                log.info("Cookie 已保存：key={}", cookieKey);
            } catch (IOException e) {
                log.error("Cookie 序列化失败：{}", e.getMessage(), e);
            }

            // 5. 生成 Token
            String token = tokenUtil.generateToken(userId);

            // 6. 构建符合接口文档的返回数据
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("name", user.getUserName());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("userInfo", userInfo);

            log.info("登录成功：userId={}, token={}", userId, token);
            return Result.ok("登录成功", responseData);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            // 判断是否是登录异常
            if (errorMsg != null) {
                if (errorMsg.contains("IP_BLOCK") || errorMsg.contains("ip 被冻结")) {
                    log.error("登录失败：IP 被冻结");
                    return Result.fail("IP 被冻结，请稍后重试");
                } else if (errorMsg.contains("LOGIN_FAILURE") || errorMsg.contains("用户名或密码错误")) {
                    log.error("登录失败：用户名或密码错误");
                    return Result.fail("用户名或密码错误");
                }
            }
            log.error("登录异常：{}", e.getMessage(), e);
            return Result.fail("登录失败，请稍后重试");
        }
    }

    @Override
    public Result logout(String token) {
        try {
            // 验证 Token
            String userId = tokenUtil.validateToken(token);
            if (userId == null) {
                return Result.fail("Token 无效或已过期");
            }

            // 使 Token 失效
            tokenUtil.invalidateToken(token);

            // 删除 Redis 中的 Cookie
            String cookieKey = KeyDescription.SSFW + userId;
            redisService.isExist(cookieKey); // 检查是否存在

            log.info("用户登出：userId={}", userId);
            return Result.ok();
        } catch (Exception e) {
            log.error("登出失败：{}", e.getMessage(), e);
            return Result.fail("登出失败，请稍后重试");
        }
    }

    @Override
    public Result getUserInfoByToken(String token) {
        try {
            // 验证 Token
            String userId = tokenUtil.validateToken(token);
            if (userId == null) {
                return Result.fail("Token 无效或已过期");
            }

            // 查询用户信息
            TUser user = this.getById(userId);
            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 返回用户信息（排除敏感字段）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("creditScore", user.getCreditScore());
            userInfo.put("creditStar", user.getCreditStar());

            return Result.ok(userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败：{}", e.getMessage(), e);
            return Result.fail("获取用户信息失败，请稍后重试");
        }
    }

    @Override
    public Result getUserInfo() {
        try {
            String userId = UserContext.getUserId();
            log.info("get user info：{}", userId);
            // 查询用户信息
            TUser user = this.getById(userId);
            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("name", user.getUserName());
            data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            data.put("studentId", user.getUserId());
            data.put("creditScore", user.getCreditScore() != null ? user.getCreditScore() : 100);
            data.put("creditStar", user.getCreditStar() != null ? user.getCreditStar() : new BigDecimal("5.0"));

            log.info("获取用户信息成功：userId={}, name={}", userId, user.getUserName());
            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("获取用户信息失败：{}", e.getMessage(), e);
            return Result.fail("获取用户信息失败，请稍后重试");
        }
    }

    @Override
    public Result getFavorites(Integer page, Integer size) {
        try {
            // 1. 获取当前用户 ID
            String userId = UserContext.getUserId();
            log.info("获取收藏列表：userId={}, page={}, size={}", userId, page, size);

            // 2. 分页查询收藏记录
            LambdaQueryWrapper<TFavorite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TFavorite::getUserId, userId)
                    .eq(TFavorite::getIsDelete, 0)
                    .orderByDesc(TFavorite::getCreateTime);

            Page<TFavorite> resultPage = favoriteService.page(new Page<>(page, size), queryWrapper);
            List<TFavorite> favorites = resultPage.getRecords();

            if (favorites.isEmpty()) {
                return Result.ok("查询成功", Map.of("total", 0, "pages", 0, "list", List.of()));
            }

            // 3. 批量提取 ID
            List<Long> goodsIds = favorites.stream().map(TFavorite::getGoodsId).collect(Collectors.toList());

            // 4. 批量查询商品信息 (转换成 Map)
            List<TGoods> goodsList = goodsService.list(new LambdaQueryWrapper<TGoods>()
                    .in(TGoods::getGoodsId, goodsIds)
                    .eq(TGoods::getIsDelete, 0));
            Map<Long, TGoods> goodsMap = goodsList.stream()
                    .collect(Collectors.toMap(TGoods::getGoodsId, g -> g));

            // 5. 批量查询图片 (转换成 Map)
            Map<Long, String> imageMap = new HashMap<>();
            for (Long gid : goodsIds) {
                imageMap.put(gid, getGoodsMainImage(gid));
            }
            // 6. 批量查询发布者 (转换成 Map)
            List<String> publisherIds = goodsList.stream()
                    .map(TGoods::getUserId).distinct().collect(Collectors.toList());
            Map<String, TUser> userMap = new HashMap<>();
            if (!publisherIds.isEmpty()) {
                userMap = this.listByIds(publisherIds).stream()
                        .collect(Collectors.toMap(TUser::getUserId, u -> u));
            }

            // 7. 组装结果
            List<Map<String, Object>> list = new ArrayList<>();
            for (TFavorite favorite : favorites) {
                TGoods goods = goodsMap.get(favorite.getGoodsId());
                if (goods == null) continue; // 商品可能被删了
                Map<String, Object> item = new HashMap<>();
                item.put("id", goods.getGoodsId());
                item.put("name", goods.getGoodsName());
                item.put("remark", goods.getGoodsNote() != null ? goods.getGoodsNote() : "");
                item.put("price", goods.getPrice());

                // 从 imageMap 里根据 ID 拿
                item.put("imgUrl", imageMap.getOrDefault(goods.getGoodsId(), ""));
                item.put("publishTime", goods.getCreateTime());
                TUser publisher = userMap.get(goods.getUserId());
                item.put("publisherName", publisher != null ? publisher.getUserName() : "未知");
                item.put("publisherId", goods.getUserId());

                list.add(item);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);

            return Result.ok("请求成功", data);

        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return Result.fail("获取收藏列表失败");
        }
    }

    @Override
    public Result getSellGoods(Integer page, Integer size,String goodsStatus) {
        try {
            if (goodsStatus == null) {
                return Result.fail("参数错误：goodsStatus不能为空");
            }
            Integer goodsStatusInt = STATUS_STR_TO_INT.get(goodsStatus);
            if (goodsStatusInt == null) {
                return Result.fail("参数错误");
            }
            // 获取当前登录用户 ID
            String userId = UserContext.getUserId();
            log.info("获取出售的商品列表：userId={}, page={}, size={}, goodsStatus={}", userId, page, size, goodsStatus);

            // 查询用户发布的商品列表
            LambdaQueryWrapper<TGoods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TGoods::getUserId, userId)
                    .eq(TGoods::getIsDelete, 0)
                    .eq(TGoods::getGoodsStatus, goodsStatusInt)
                    .eq(TGoods::getGoodsType,1)
                    .orderByDesc(TGoods::getCreateTime);

            Page<TGoods> goodsPage = new Page<>(page, size);
            Page<TGoods> resultPage = goodsService.page(goodsPage, queryWrapper);
            List<TGoods> goodsList = resultPage.getRecords();

            // 组装结果
            List<Map<String, Object>> list = new ArrayList<>();
            for (TGoods goods : goodsList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", goods.getGoodsId());
                item.put("name", goods.getGoodsName());
                item.put("remark", goods.getGoodsNote() != null ? goods.getGoodsNote() : "");
                item.put("price", goods.getPrice());

                // 获取商品主图
                String mainImg = getGoodsMainImage(goods.getGoodsId());
                item.put("imgUrl", mainImg != null ? mainImg : "");
                // 发布时间
                if (goods.getCreateTime() != null) {
                    item.put("publishTime",goods.getCreateTime());
                } else {
                    item.put("publishTime", "");
                }
                // 查询发布者信息
                item.put("publisherName", UserContext.getUsername() != null ? UserContext.getUsername() : "未知");
                item.put("publisherId", goods.getUserId());
                item.put("goodsStatus",  STATUS_INT_TO_STR.getOrDefault(goods.getGoodsStatus(), "未知"));
                list.add(item);
            }
            // 计算总页数
            long total = resultPage.getTotal();
            long pages = resultPage.getPages();

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("pages", pages);
            data.put("list", list);

            log.info("获取出售的商品列表成功：userId={}, total={}, pages={}", userId, total, pages);
            return Result.ok("请求成功", data);

        } catch (Exception e) {
            log.error("获取出售的商品列表失败：{}", e.getMessage(), e);
            return Result.fail("获取出售的商品列表失败，请稍后重试");
        }
    }

    /**
     * 获取商品主图
     */
    private String getGoodsMainImage(Long goodsId) {
        try {
            LambdaQueryWrapper<TGoodsImage> imageQuery = new LambdaQueryWrapper<>();
            imageQuery.eq(TGoodsImage::getGoodsId, goodsId)
                    .orderByAsc(TGoodsImage::getSort)
                    .last("LIMIT 1");

            TGoodsImage goodsImage = goodsImageService.getOne(imageQuery);
            return goodsImage != null ? goodsImage.getImageUrl() : null;
        } catch (Exception e) {
            log.error("获取商品图片失败：goodsId={}, error={}", goodsId, e.getMessage());
            return null;
        }
    }


    @Override
    public Result getBoughtGoods(Integer page, Integer size) {
        try {
            // 1. 获取当前登录用户 ID
            String userId = UserContext.getUserId();
            log.info("获取买到的商品列表：userId={}, page={}, size={}", userId, page, size);

            // 2. 查询用户作为买家的交易记录
            LambdaQueryWrapper<TTrade> tradeQuery = new LambdaQueryWrapper<>();
            tradeQuery.eq(TTrade::getBuyerId, userId)
                    .eq(TTrade::getIsDelete, 0)
                    .orderByDesc(TTrade::getTradeTime);

            Page<TTrade> tradePage = new Page<>(page, size);
            Page<TTrade> resultPage = tradeService.page(tradePage, tradeQuery);
            List<TTrade> tradeList = resultPage.getRecords();

            if (tradeList.isEmpty()) {
                return Result.ok("查询成功", Map.of("total", 0, "pages", 0, "list", List.of()));
            }

            // 3. 批量提取商品ID和交易ID
            List<Long> goodsIds = tradeList.stream().map(TTrade::getGoodsId).collect(Collectors.toList());
            List<Long> tradeIds = tradeList.stream().map(TTrade::getTradeId).collect(Collectors.toList());

            // 4. 批量查询商品信息
            List<TGoods> goodsList = goodsService.list(new LambdaQueryWrapper<TGoods>()
                    .in(TGoods::getGoodsId, goodsIds)
                    .eq(TGoods::getIsDelete, 0));
            Map<Long, TGoods> goodsMap = goodsList.stream()
                    .collect(Collectors.toMap(TGoods::getGoodsId, g -> g));

            // 5. 批量查询商品主图
            Map<Long, String> imageMap = new HashMap<>();
            for (Long goodsId : goodsIds) {
                imageMap.put(goodsId, getGoodsMainImage(goodsId));
            }

            // 6. 批量查询卖家信息
            List<String> sellerIds = tradeList.stream().map(TTrade::getSellerId).distinct().collect(Collectors.toList());
            Map<String, TUser> sellerMap = new HashMap<>();
            if (!sellerIds.isEmpty()) {
                sellerMap = this.listByIds(sellerIds).stream()
                        .collect(Collectors.toMap(TUser::getUserId, u -> u));
            }

            // 7. 批量查询评价状态（检查是否已评价）
            Map<Long, Boolean> evaluatedMap = new HashMap<>();
            if (!tradeIds.isEmpty()) {
                List<TEvaluate> evaluateList = evaluateService.list(new LambdaQueryWrapper<TEvaluate>()
                        .in(TEvaluate::getTradeId, tradeIds)
                        .eq(TEvaluate::getIsDelete, 0));
                // 将已评价的交易ID存入Set
                Set<Long> evaluatedTradeIds = evaluateList.stream()
                        .map(TEvaluate::getTradeId)
                        .collect(Collectors.toSet());
                // 构建交易ID到评价状态的映射
                for (TTrade trade : tradeList) {
                    evaluatedMap.put(trade.getTradeId(), evaluatedTradeIds.contains(trade.getTradeId()));
                }
            }

            // 8. 组装结果
            List<Map<String, Object>> list = new ArrayList<>();
            for (TTrade trade : tradeList) {
                TGoods goods = goodsMap.get(trade.getGoodsId());
                if (goods == null) continue; // 商品可能被删除

                Map<String, Object> item = new HashMap<>();
                item.put("id", goods.getGoodsId());
                item.put("name", goods.getGoodsName());
                item.put("remark", goods.getGoodsNote() != null ? goods.getGoodsNote() : "");
                item.put("price", trade.getTradePrice() != null ? trade.getTradePrice() : goods.getPrice());

                // 商品主图
                item.put("imgUrl", imageMap.getOrDefault(goods.getGoodsId(), ""));

                // 交易完成时间
                if (trade.getTradeTime() != null) {
                    item.put("tradeTime", trade.getTradeTime());
                } else {
                    item.put("tradeTime", "");
                }

                // 卖家信息
                TUser seller = sellerMap.get(trade.getSellerId());
                item.put("sellerId", trade.getSellerId());
                item.put("sellerName", seller != null ? seller.getUserName() : "未知");

                // 是否已评价
                item.put("isEvaluated", evaluatedMap.getOrDefault(trade.getTradeId(), false));

                list.add(item);
            }

            // 9. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);

            log.info("获取买到的商品列表成功：userId={}, total={}, pages={}", userId, resultPage.getTotal(), resultPage.getPages());
            return Result.ok("请求成功", data);

        } catch (Exception e) {
            log.error("获取买到的商品列表失败：{}", e.getMessage(), e);
            return Result.fail("获取买到的商品列表失败，请稍后重试");
        }
    }

    @Override
    public Result getBuyGoods(Integer page, Integer size, String status) {
        try {
            if (status == null) {
                return Result.fail("参数错误：status不能为空");
            }
            Integer goodsStatusInt = STATUS_STR_TO_INT.get(status);
            if (goodsStatusInt == null || (goodsStatusInt != 0 && goodsStatusInt != 2)) {
                return Result.fail("参数错误：status只能为online或offline");
            }

            // 获取当前登录用户 ID
            String userId = UserContext.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录或会话已过期");
            }
            // 验证分页参数
            if (page == null || page < 1) {
                page = 1;
            }
            if (size == null || size < 1 || size > 100) {
                size = 10; // 默认每页10条，最大100条
            }
            log.info("获取预购商品列表：userId={}, page={}, size={}, status={}", userId, page, size, status);
            // 查询用户发布的预购商品列表
            LambdaQueryWrapper<TGoods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TGoods::getUserId, userId)
                    .eq(TGoods::getIsDelete, 0)
                    .eq(TGoods::getGoodsStatus, goodsStatusInt)
                    .eq(TGoods::getGoodsType, 2)
                    .orderByDesc(TGoods::getCreateTime);

            Page<TGoods> goodsPage = new Page<>(page, size);
            Page<TGoods> resultPage = goodsService.page(goodsPage, queryWrapper);
            List<TGoods> goodsList = resultPage.getRecords();

            // 批量查询商品主图
            List<Long> goodsIds = goodsList.stream().map(TGoods::getGoodsId).collect(Collectors.toList());
            Map<Long, String> imageMap = new HashMap<>();
            for (Long goodsId : goodsIds) {
                imageMap.put(goodsId, getGoodsMainImage(goodsId));
            }

            // 组装结果
            List<Map<String, Object>> list = new ArrayList<>();
            for (TGoods goods : goodsList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", goods.getGoodsId());
                item.put("name", goods.getGoodsName());
                item.put("remark", goods.getGoodsNote() != null ? goods.getGoodsNote() : "");
                item.put("price", goods.getPrice());

                // 获取商品主图
                String mainImg = imageMap.getOrDefault(goods.getGoodsId(), "");
                item.put("imgUrl", mainImg);

                // 发布时间
                if (goods.getCreateTime() != null) {
                    item.put("publishTime", goods.getCreateTime());
                } else {
                    item.put("publishTime", "");
                }

                // 查询发布者信息
                item.put("publisherName", UserContext.getUsername() != null ? UserContext.getUsername() : "未知");
                item.put("publisherId", goods.getUserId());
                item.put("status", STATUS_INT_TO_STR.getOrDefault(goods.getGoodsStatus(), "未知"));
                list.add(item);
            }

            // 计算总页数
            long total = resultPage.getTotal();
            long pages = resultPage.getPages();

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("pages", pages);
            data.put("list", list);

            log.info("获取预购商品列表成功：userId={}, total={}, pages={}", userId, total, pages);
            return Result.ok("请求成功", data);

        }
        catch (IllegalArgumentException e) {
            log.error("获取预购商品列表参数错误：{}", e.getMessage(), e);
            return Result.fail("参数错误：" + e.getMessage());
        }
        catch (Exception e) {
            log.error("获取预购商品列表失败：{}", e.getMessage(), e);
            return Result.fail("获取预购商品列表失败，请稍后重试");
        }
    }
    /**
     * 编辑我的商品/预购需求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result editGoods(Long goodsId, GoodsEditRequest request) {
        try {
            log.info("开始编辑商品，goodsId: {}, 请求参数: {}", goodsId, request);

            // 1. 验证商品是否存在
            TGoods goods = goodsService.getById(goodsId);
            if (goods == null) {
                log.warn("商品不存在，goodsId: {}", goodsId);
                return Result.fail("商品不存在");
            }

            // 2. 验证当前用户是否有权限编辑
            String currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                log.warn("用户未登录");
                return Result.fail("用户未登录");
            }

            if (!currentUserId.equals(goods.getUserId())) {
                log.warn("用户无权限编辑该商品，当前用户: {}, 商品所有者: {}", currentUserId, goods.getUserId());
                return Result.fail("无权限编辑该商品");
            }

            // 3. 验证商品状态是否允许编辑（已售出或已下架的商品不允许编辑）
            if (goods.getGoodsStatus() == 1 || goods.getGoodsStatus() == 2) {
                log.warn("商品状态不允许编辑，goodsId: {}, 状态: {}", goodsId, goods.getGoodsStatus());
                return Result.fail("该商品状态不允许编辑");
            }

            // 4. 更新商品基本信息
            goods.setGoodsName(request.getName());
            goods.setGoodsDesc(request.getDesc());
            goods.setGoodsNote(request.getRemark());
            goods.setPrice(request.getPrice());
            goods.setUseScene(request.getPurpose());
            goods.setExchangePlace(request.getExchangeAddr());

            // 转换商品类型：sell->1, buy->2
            Integer goodsType = "sell".equals(request.getType()) ? 1 : 2;
            goods.setGoodsType(goodsType);

            // 如果商品在审核状态，编辑后需要重新审核
            if (goods.getGoodsStatus() == 3) {
                goods.setGoodsStatus(3); // 保持审核状态
            }

            boolean updateResult = goodsService.updateById(goods);
            if (!updateResult) {
                log.error("更新商品信息失败，goodsId: {}", goodsId);
                return Result.fail("更新商品信息失败");
            }

            // 5. 处理商品图片
            if (request.getImgUrls() != null && !request.getImgUrls().isEmpty()) {
                // 删除旧的商品图片
                LambdaQueryWrapper<TGoodsImage> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(TGoodsImage::getGoodsId, goodsId);
                goodsImageService.remove(deleteWrapper);

                // 添加新的商品图片
                List<TGoodsImage> newImages = new ArrayList<>();
                for (int i = 0; i < request.getImgUrls().size(); i++) {
                    TGoodsImage image = new TGoodsImage();
                    image.setGoodsId(goodsId);
                    image.setImageUrl(request.getImgUrls().get(i));
                    image.setSort(i);
                    newImages.add(image);
                }
                if (!newImages.isEmpty()) {
                    goodsImageService.saveBatch(newImages);
                }
            }

            log.info("商品编辑成功，goodsId: {}", goodsId);
            return Result.ok("编辑成功",null);

        } catch (Exception e) {
            log.error("编辑商品异常，goodsId: {}", goodsId, e);
            return Result.fail("编辑失败，系统异常");
        }
    }
    /**
     * 下架我的商品/预购需求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result offlineGoods(Long goodsId) {
        try {
            log.info("开始下架商品，goodsId: {}", goodsId);

            // 1. 验证商品是否存在
            TGoods goods = goodsService.getById(goodsId);
            if (goods == null) {
                log.warn("商品不存在，goodsId: {}", goodsId);
                return Result.fail("商品不存在");
            }

            // 2. 验证当前用户是否有权限下架
            String currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                log.warn("用户未登录");
                return Result.fail("用户未登录");
            }

            if (!currentUserId.equals(goods.getUserId())) {
                log.warn("用户无权限下架该商品，当前用户: {}, 商品所有者: {}", currentUserId, goods.getUserId());
                return Result.fail("无权限下架该商品");
            }

            // 3. 验证商品状态是否允许下架
            // 已售出(1)或已下架(2)的商品不能重复下架
            if (goods.getGoodsStatus() == 1) {
                log.warn("已售出的商品不能下架，goodsId: {}", goodsId);
                return Result.fail("已售出的商品不能下架");
            }

            if (goods.getGoodsStatus() == 2) {
                log.warn("商品已下架，无需重复操作，goodsId: {}", goodsId);
                return Result.fail("商品已下架");
            }

            // 4. 更新商品状态为下架(2)
            LambdaUpdateWrapper<TGoods> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TGoods::getGoodsId, goodsId)
                    .set(TGoods::getGoodsStatus, 2); // offline状态

            boolean updateResult = goodsService.update(updateWrapper);
            if (!updateResult) {
                log.error("下架商品失败，goodsId: {}", goodsId);
                return Result.fail("下架失败");
            }

            log.info("商品下架成功，goodsId: {}", goodsId);
            return Result.ok("下架成功",null);

        } catch (Exception e) {
            log.error("下架商品异常，goodsId: {}", goodsId, e);
            return Result.fail("下架失败，系统异常");
        }
    }

    /**
     * 删除我的商品/预购需求（软删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteGoods(Long goodsId) {
        try {
            log.info("开始删除商品，goodsId: {}", goodsId);

            // 1. 验证商品是否存在
            TGoods goods = goodsService.getById(goodsId);
            if (goods == null) {
                log.warn("商品不存在，goodsId: {}", goodsId);
                return Result.fail("商品不存在");
            }

            // 2. 验证当前用户是否有权限删除
            String currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                log.warn("用户未登录");
                return Result.fail("用户未登录");
            }

            if (!currentUserId.equals(goods.getUserId())) {
                log.warn("用户无权限删除该商品，当前用户: {}, 商品所有者: {}", currentUserId, goods.getUserId());
                return Result.fail("无权限删除该商品");
            }

            // 3. 验证商品状态是否允许删除
            // 已售出的商品不允许删除
            if (goods.getGoodsStatus() == 1) {
                log.warn("已售出的商品不能删除，goodsId: {}", goodsId);
                return Result.fail("已售出的商品不能删除");
            }

            // 4. 软删除商品（更新isDelete字段）
            LambdaUpdateWrapper<TGoods> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TGoods::getGoodsId, goodsId)
                    .set(TGoods::getIsDelete, 1); // 1表示已删除

            boolean updateResult = goodsService.update(updateWrapper);
            if (!updateResult) {
                log.error("删除商品失败，goodsId: {}", goodsId);
                return Result.fail("删除失败");
            }

            // 5. 删除商品相关的图片记录
            LambdaQueryWrapper<TGoodsImage> imageWrapper = new LambdaQueryWrapper<>();
            imageWrapper.eq(TGoodsImage::getGoodsId, goodsId);
            goodsImageService.remove(imageWrapper);

            log.info("商品删除成功，goodsId: {}", goodsId);
            return Result.ok("删除成功",null);

        } catch (Exception e) {
            log.error("删除商品异常，goodsId: {}", goodsId, e);
            return Result.fail("删除失败，系统异常");
        }
    }

    @Override
    public Result getUserHomeInfo(String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail(400, "用户ID参数错误");
            }

            log.info("获取用户主页信息：userId={}", userId);

            // 1. 查询目标用户信息
            TUser targetUser = this.getById(userId);
            if (targetUser == null || targetUser.getIsDelete() == 1) {
                log.warn("用户不存在或已删除：userId={}", userId);
                return Result.fail(404, "用户不存在");
            }

            // 2. 构建用户基本信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", targetUser.getUserId());
            userInfo.put("name", targetUser.getUserName());
            userInfo.put("creditScore", targetUser.getCreditScore() != null ? targetUser.getCreditScore() : 100);
            userInfo.put("creditStar", targetUser.getCreditStar() != null ? targetUser.getCreditStar() : new BigDecimal("5.0"));

            // 3. 查询用户的商品（分类）
            LambdaQueryWrapper<TGoods> goodsWrapper = new LambdaQueryWrapper<>();
            goodsWrapper.eq(TGoods::getUserId, userId)
                    .eq(TGoods::getIsDelete, 0)
                    .orderByDesc(TGoods::getCreateTime);
            List<TGoods> allGoods = goodsService.list(goodsWrapper);

            // 3.1 分离出售商品和预购商品
            List<TGoods> sellGoods = new ArrayList<>();
            List<TGoods> buyGoods = new ArrayList<>();
            for (TGoods goods : allGoods) {
                if (goods.getGoodsType() != null && goods.getGoodsType() == 1) {
                    sellGoods.add(goods);
                } else if (goods.getGoodsType() != null && goods.getGoodsType() == 2) {
                    buyGoods.add(goods);
                }
            }

            // 3.2 按状态分类出售商品
            List<TGoods> sellOnline = new ArrayList<>();
            List<TGoods> sellSold = new ArrayList<>();
            for (TGoods goods : sellGoods) {
                if (goods.getGoodsStatus() != null && goods.getGoodsStatus() == 0) { // online
                    sellOnline.add(goods);
                } else if (goods.getGoodsStatus() != null && goods.getGoodsStatus() == 1) { // sold
                    sellSold.add(goods);
                }
            }

            // 3.3 过滤预购在线商品
            List<TGoods> buyOnline = buyGoods.stream()
                    .filter(g -> g.getGoodsStatus() != null && g.getGoodsStatus() == 0)
                    .collect(Collectors.toList());

            // 4. 批量查询商品图片
            List<Long> allGoodsIds = allGoods.stream().map(TGoods::getGoodsId).toList();
            Map<Long, String> imageMap = new HashMap<>();
            if (!allGoodsIds.isEmpty()) {
                // TODO N+1问题：这里循环调用getGoodsMainImage方法，实际会导致N+1查询。后续可以优化为一次查询所有图片，存入Map中再取用，提示性能。
                for (Long goodsId : allGoodsIds) {
                    imageMap.put(goodsId, getGoodsMainImage(goodsId));
                }
            }

            // 5. 组装商品数据
            List<Map<String, Object>> sellOnlineList = buildGoodsList(sellOnline, imageMap);
            List<Map<String, Object>> sellSoldList = buildGoodsList(sellSold, imageMap);
            List<Map<String, Object>> buyOnlineList = buildGoodsList(buyOnline, imageMap);

            // 6. 查询用户的评价（被评价）
            LambdaQueryWrapper<TEvaluate> evalWrapper = new LambdaQueryWrapper<>();
            evalWrapper.eq(TEvaluate::getSellerId, userId)
                    .eq(TEvaluate::getIsDelete, 0)
                    .orderByDesc(TEvaluate::getCreateTime);
            List<TEvaluate> evaluates = evaluateService.list(evalWrapper);

            // 7. 查询评价者信息（批量）
            Set<String> evaluatorIds = new HashSet<>();
            for (TEvaluate eval : evaluates) {
                if (eval.getSellerId().equals(userId)) {
                    evaluatorIds.add(eval.getBuyerId());
                }
            }
            Map<String, TUser> evaluatorMap = new HashMap<>();
            if (!evaluatorIds.isEmpty()) {
                evaluatorMap = this.listByIds(new ArrayList<>(evaluatorIds)).stream()
                        .collect(Collectors.toMap(TUser::getUserId, u -> u));
            }

            // 8. 构建评价列表
            List<Map<String, Object>> evaluateList = new ArrayList<>();
            double totalScore = 0;
            for (TEvaluate eval : evaluates) {
                // 确定评价者（对匿名评价特殊处理）
                String evaluatorId = eval.getBuyerId();
                String evaluatorName = "匿名用户";
                if (eval.getIsAnonymous() == 0) {  // 非匿名
                    TUser evaluator = evaluatorMap.get(evaluatorId);
                    evaluatorName = evaluator != null ? evaluator.getUserName() : "未知用户";
                }

                Map<String, Object> evalItem = new HashMap<>();
                evalItem.put("evalId", eval.getEvalId());
                evalItem.put("totalScore", eval.getTotalScore() != null ? eval.getTotalScore() : 0);
                evalItem.put("descScore", eval.getDescScore() != null ? eval.getDescScore() : 0);
                evalItem.put("commScore", eval.getCommScore() != null ? eval.getCommScore() : 0);
                evalItem.put("evalContent", eval.getEvalContent() != null ? eval.getEvalContent() : "");
                evalItem.put("createTime", eval.getCreateTime());
                evalItem.put("buyerName", evaluatorName);

                evaluateList.add(evalItem);
                totalScore += (eval.getTotalScore() != null ? eval.getTotalScore() : 0);
            }

            // 9. 计算评价统计
            int evaluateCount = evaluates.size();
            BigDecimal avgScore = BigDecimal.ZERO;
            if (evaluateCount > 0) {
                avgScore = BigDecimal.valueOf(totalScore / evaluateCount).setScale(2, java.math.RoundingMode.HALF_UP);
            }

            // 10. 构建完整返回数据
            Map<String, Object> goodsMap = new HashMap<>();
            goodsMap.put("sellOnline", sellOnlineList);
            goodsMap.put("sellSold", sellSoldList);
            goodsMap.put("buyOnline", buyOnlineList);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userInfo", userInfo);
            responseData.put("goods", goodsMap);
            responseData.put("evaluates", evaluateList);
            responseData.put("evaluateCount", evaluateCount);
            responseData.put("avgScore", avgScore);

            log.info("获取用户主页信息成功：userId={}, evaluateCount={}, avgScore={}", userId, evaluateCount, avgScore);
            return Result.ok("请求成功", responseData);

        } catch (Exception e) {
            log.error("获取用户主页信息失败：userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("获取用户主页信息失败，请稍后重试");
        }
    }

    /**
     * 辅助方法：构建商品列表
     */
    private List<Map<String, Object>> buildGoodsList(List<TGoods> goods, Map<Long, String> imageMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TGoods g : goods) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getGoodsId());
            item.put("name", g.getGoodsName());
            item.put("remark", g.getGoodsNote() != null ? g.getGoodsNote() : "");
            item.put("price", g.getPrice());
            item.put("imgUrl", imageMap.getOrDefault(g.getGoodsId(), ""));
            item.put("publishTime", g.getCreateTime());
            item.put("publisherName", "");  // 主页中所有商品都是该用户的，所以直接放空
            item.put("publisherId", g.getUserId());
            result.add(item);
        }
        return result;
    }
    @Override
    public PublisherDTO getPublisherInfo(String userId) {
        // 查询用户信息
        TUser user = this.getById(userId);
        if (user == null) {
            return null;
        }

        // 构建发布者信息DTO
        PublisherDTO publisher = new PublisherDTO();
        publisher.setId(user.getUserId());
        publisher.setName(user.getUserName());
        publisher.setCreditScore(user.getCreditScore());
        // 将 BigDecimal 转换为 Double
        if (user.getCreditStar() != null) {
            publisher.setCreditStar(user.getCreditStar().doubleValue());
        }

        return publisher;
    }
}