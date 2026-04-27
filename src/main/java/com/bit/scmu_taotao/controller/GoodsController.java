package com.bit.scmu_taotao.controller;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bit.scmu_taotao.config.storage.S3StorageProperties;
import com.bit.scmu_taotao.dto.goods.GoodsRequestDTO;
import com.bit.scmu_taotao.dto.goods.GoodsResponseDTO;
import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import com.bit.scmu_taotao.dto.goods.SearchRequestDTO;
import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.service.storage.ObjectStorageService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.util.storage.ObjectKeyParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private TGoodsService tGoodsService;

    @Autowired
    private TUserService tUserService;

    @Autowired
    private TFavoriteService tFavoriteService;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private ChatSessionService chatSessionService;
    @Autowired
    private TGoodsImageService tGoodsImageService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private TGoodsCategoryService tGoodsCategoryService;

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private S3StorageProperties s3StorageProperties;

    @Autowired
    private SensitiveWordService sensitiveWordService;
    /**
     * 首页Tab商品列表
     * 支持tab参数，category作为兼容参数
     */
    @GetMapping
    public Result getHomeGoodsList(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String currentUserId = UserContext.getUserId();
        return tGoodsService.getHomeGoodsList(tab, category, page, size, currentUserId);
    }

    /**
     * 首页模糊搜索（匹配商品名称、描述、备注）
     */
    @GetMapping("/search")
    public Result searchHomeGoods(@Valid SearchRequestDTO query, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "参数错误";
            return Result.fail(400, msg);
        }

        log.info("首页搜索请求：keyword={}, page={}, size={}", query.getKeyword(), query.getPage(), query.getSize());
        return tGoodsService.searchHomeGoods(query.getKeyword(), query.getPage(), query.getSize());
    }

    @PostMapping
    public Result publishGoods(@RequestBody Map<String, Object> request) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        // 2. 解析请求参数
        String name = (String) request.get("name");
        String desc = (String) request.get("desc");
        String remark = (String) request.get("remark");
        double price = Double.parseDouble(request.get("price").toString());
        String purpose = (String) request.get("purpose");
        String exchangeAddr = (String) request.get("exchangeAddr");
        List<String> imgUrls = (List<String>) request.get("imgUrls");
        String type = (String) request.get("type");
        String category = (String) request.get("category");
        String draftId = (String) request.get("draftId"); // 获取草稿 ID

        // 3. 查询商品类别id
        QueryWrapper<TGoodsCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_name", category);
        TGoodsCategory goodsCategory = tGoodsCategoryService.getOne(wrapper);
        if (goodsCategory == null) {
            return Result.fail("商品分类不存在");
        }
        log.info("开始检测商品信息敏感词");
        sensitiveWordService.validateGoods(name, desc, remark, purpose, exchangeAddr);
        // 4. 创建商品实体
        TGoods goods = new TGoods();
        goods.setUserId(userId);
        goods.setCategoryId(goodsCategory.getCategoryId()); // 设置分类 ID
        goods.setGoodsName(name);
        goods.setGoodsDesc(desc);
        goods.setGoodsNote(remark);
        goods.setPrice(java.math.BigDecimal.valueOf(price));
        goods.setUseScene(purpose);
        goods.setExchangePlace(exchangeAddr);
        goods.setGoodsType("sell".equals(type) ? 1 : 2); // 1=出售, 2=预购
        goods.setGoodsStatus(0); // 0=在售
        goods.setIsDelete(0);

        // 5. 保存商品
        tGoodsService.save(goods);

        // 6. 保存商品图片
        if (imgUrls != null && !imgUrls.isEmpty()) {
            int sort = 1;
            for (String imgUrl : imgUrls) {
                TGoodsImage goodsImage = new TGoodsImage();
                goodsImage.setGoodsId(goods.getGoodsId());
                goodsImage.setImageUrl(imgUrl);
                goodsImage.setSort(sort++);
                tGoodsImageService.save(goodsImage);
            }
        }

        // 7. 如果提供了草稿 ID，删除对应的草稿
        if (draftId != null && !draftId.isEmpty()) {
            String draftKey = "goods_draft:" + userId + ":" + draftId;
            String draftIdsKey = "goods_draft:" + userId + ":ids";
            redisService.removeFromSet(draftIdsKey, draftId);
            redisService.delete(draftKey);
        }

        // 8. 构建响应数据
        java.util.Map<String, Object> data = new java.util.HashMap<>();
//        data.put("goodsId", goods.getGoodsId());
        data.put("imgUrls",imgUrls);

        // 9. 返回成功响应
        return Result.ok("发布成功，平台将进行合规审核", data);
    }


    // 保存草稿接口
    @PostMapping("/draft")
    public Result saveDraft(@RequestBody java.util.Map<String, Object> request) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        // 2. 解析请求参数
        String draftId = (String) request.get("draftId");
        String name = (String) request.get("name");
        String desc = (String) request.get("desc");
        String remark = (String) request.get("remark");
        double price = Double.parseDouble(request.get("price").toString());
        String purpose = (String) request.get("purpose");
        String exchangeAddr = (String) request.get("exchangeAddr");
        java.util.List<String> imgUrls = (java.util.List<String>) request.get("imgUrls");
        String type = (String) request.get("type");

        // 3. 构建 GoodsRequestDTO
        GoodsRequestDTO goodsRequest = new GoodsRequestDTO();
        goodsRequest.setName(name);
        goodsRequest.setDesc(desc);
        goodsRequest.setRemark(remark);
        goodsRequest.setPrice(price);
        goodsRequest.setPurpose(purpose);
        goodsRequest.setExchangeAddr(exchangeAddr);
        goodsRequest.setImgUrls(imgUrls);
        goodsRequest.setType(type);

        // 4. 判断是新建还是修改操作
        if (draftId == null || draftId.isEmpty()) {
            // 新建操作
            draftId = UUID.randomUUID().toString();
            String draftKey = "goods_draft:" + userId + ":" + draftId;
            String draftIdsKey = "goods_draft:" + userId + ":ids";

            // 保存到 Redis，设置 24 小时过期
            redisService.setWithExpire(draftKey, goodsRequest, 24, TimeUnit.HOURS);

            // 将草稿 ID 添加到集合中
            redisService.addToSet(draftIdsKey, draftId);
            // 为集合设置过期时间
            redisService.setExpire(draftIdsKey, 24, TimeUnit.HOURS);
        } else {
            // 修改操作
            String draftKey = "goods_draft:" + userId + ":" + draftId;
            // 检查草稿是否存在
            if (redisService.get(draftKey) == null) {
                return Result.fail(404, "草稿不存在或已过期");
            }
            // 更新草稿，设置 24 小时过期
            redisService.setWithExpire(draftKey, goodsRequest, 24, TimeUnit.HOURS);
        }

        // 5. 构建响应数据
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("draftId", draftId);

        // 6. 返回成功响应
        return Result.ok("草稿保存成功", data);
    }


    // 获取草稿列表接口
    @GetMapping("/draft")
    public Result getDraftList() {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        // 2. 构建 Redis 键
        String draftIdsKey = "goods_draft:" + userId + ":ids";

        // 3. 获取所有草稿 ID
        //TODO 返回简要信息用于列表显示
        Set<Object> draftIds = redisService.getSetMembers(draftIdsKey);

        // 4. 构建响应数据
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("draftIds", draftIds);

        // 5. 返回成功响应
        return Result.ok("获取草稿列表成功", data);
    }

    // 获取草稿详情接口
    @GetMapping("/draft/{draftId}")
    public Result getDraftDetail(@PathVariable("draftId") String draftId) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        // 2. 构建 Redis 键
        String draftKey = "goods_draft:" + userId + ":" + draftId;

        // 3. 从 Redis 获取草稿详情
        GoodsRequestDTO draft = (GoodsRequestDTO) redisService.get(draftKey);
        if (draft == null) {
            return Result.fail(404, "草稿不存在或已过期");
        }

        // 4. 构建响应数据
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("draft", draft);

        // 5. 返回成功响应
        return Result.ok("获取草稿详情成功", data);
    }

    // 删除草稿接口
    @DeleteMapping("/draft/{draftId}")
    public Result deleteDraft(@PathVariable("draftId") String draftId) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        // 2. 构建 Redis 键
        String draftKey = "goods_draft:" + userId + ":" + draftId;
        String draftIdsKey = "goods_draft:" + userId + ":ids";

        // 先拿草稿内容，用于清理图片
        Object draftObj = redisService.get(draftKey);
        if (draftObj instanceof GoodsRequestDTO draft && draft.getImgUrls() != null) {
            for (String imageUrl : draft.getImgUrls()) {
                String objectKey = ObjectKeyParser.extractObjectKey(imageUrl, s3StorageProperties.getBucket());
                if (objectKey == null || objectKey.isBlank()) {
                    continue;
                }
                // 仅允许删除当前用户目录，避免误删
                if (!objectKey.startsWith("user/" + userId + "/")) {
                    log.warn("skip deleting non-user-owned object, userId={}, key={}", userId, objectKey);
                    continue;
                }
                try {
                    objectStorageService.deleteObject(objectKey);
                } catch (Exception e) {
                    // 删除失败不阻断草稿删除，避免接口整体失败
                    log.warn("failed to delete draft image, userId={}, key={}", userId, objectKey, e);
                }
            }
        }
        // 3. 从集合中移除草稿 ID
        redisService.removeFromSet(draftIdsKey, draftId);

        // 4. 删除草稿详情
        redisService.delete(draftKey);

        // 5. 返回成功响应
        return Result.ok("删除草稿成功", null);
    }

    @GetMapping("/{goodsId}")
    public Result getGoods(@PathVariable("goodsId") @Min(1) int goodsId) {
        // todo 该接口无返回值
        // 1. 获取商品基本信息
        TGoods goods = tGoodsService.getGoodsById(goodsId);

        // 异步埋点：记录浏览 + 更新点击量（防刷机制内置）
        String currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            try {
                new Thread(() -> {
                    try {
                        recommendationService.recordBrowseAndUpdateViewCount(currentUserId, (long) goodsId);
                        log.debug("Browse logging completed: userId={}, goodsId={}", currentUserId, goodsId);
                    } catch (Exception e) {
                        log.warn("Failed to record browse for goodsId={}, userId={}", goodsId, currentUserId, e);
                    }
                }).start();
            } catch (Exception e) {
                log.warn("Failed to spawn browse logging thread", e);
            }
        }

        // 2. 构建响应DTO
        GoodsResponseDTO data = new GoodsResponseDTO();
        data.setId(goods.getGoodsId());
        data.setName(goods.getGoodsName());
        data.setDesc(goods.getGoodsDesc());
        data.setPrice(goods.getPrice());
        data.setPurpose(goods.getUseScene());
        data.setExchangeAddr(goods.getExchangePlace());

        // 3. 格式化发布时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        data.setPublishTime(sdf.format(goods.getCreateTime()));

        // 4. 设置商品类型
        data.setType(goods.getGoodsType() == 1 ? "sell" : "buy");

        // 5. 获取并设置图片URLs（需要在TGoodsService中添加相应方法）
        List<String> imgUrls = tGoodsService.getGoodsImageUrls(goodsId);
        data.setImgUrls(imgUrls);

        // 6. 获取并设置发布者信息（需要在TUserService中添加相应方法）
        PublisherDTO publisher = tUserService.getPublisherInfo(goods.getUserId());
        data.setPublisher(publisher);

        return Result.ok("请求成功", data);
    }

    @PostMapping("/{goodsId}/favorite")
    public Result favorite(@PathVariable("goodsId") @Min(1) int goodsId) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        long gid = (long) goodsId;
        // 2. 查询是否已有收藏记录（包含已删除）
        LambdaQueryWrapper<TFavorite> query = new LambdaQueryWrapper<>();
        query.eq(TFavorite::getUserId, userId)
                .eq(TFavorite::getGoodsId, gid)
                .last("LIMIT 1");
        TFavorite existed = tFavoriteService.getOne(query);

        // 3. 去重处理
        if (existed != null) {
            // 已收藏，直接返回
            if (Integer.valueOf(0).equals(existed.getIsDelete())) {
                return Result.ok("已收藏，无需重复操作", null);
            }
            // 历史记录被取消过，恢复收藏
            existed.setIsDelete(0);
            tFavoriteService.updateById(existed);
            return Result.ok("收藏成功", null);
        }
        // 4. 创建收藏记录
        TFavorite favorite = new TFavorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(gid);
        favorite.setIsDelete(0);
        // 5. 保存到数据库
        tFavoriteService.save(favorite);
        // 6. 返回成功响应
        return Result.ok("收藏成功", null);
    }

    @DeleteMapping("/{goodsId}/favorite")
    public Result unfavorite(@PathVariable("goodsId") @Min(1) int goodsId) {
        // 1. 获取当前登录用户的 userId
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        long gid = (long) goodsId;

        // 2. 查询当前有效收藏记录
        LambdaQueryWrapper<TFavorite> query = new LambdaQueryWrapper<>();
        query.eq(TFavorite::getUserId, userId)
                .eq(TFavorite::getGoodsId, gid)
                .eq(TFavorite::getIsDelete, 0)
                .last("LIMIT 1");
        TFavorite existed = tFavoriteService.getOne(query);

        // 3. 幂等处理：没找到也返回取消成功
        if (existed == null) {
            return Result.ok("取消收藏成功", null);
        }

        // 4. 软删除收藏记录
        existed.setIsDelete(1);
        tFavoriteService.updateById(existed);

        // 5. 返回成功响应
        return Result.ok("取消收藏成功", null);
    }

    @GetMapping("/{goodsId}/trade")
    public Result contactSeller(@PathVariable("goodsId") @Min(1) int goodsId) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        return chatSessionService.contactSeller((long) goodsId, userId);
    }


}
