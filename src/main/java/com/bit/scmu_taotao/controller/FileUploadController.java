package com.bit.scmu_taotao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bit.scmu_taotao.config.storage.S3StorageProperties;
import com.bit.scmu_taotao.dto.upload.UploadImageResponse;
import com.bit.scmu_taotao.entity.TGoodsImage;
import com.bit.scmu_taotao.service.TGoodsImageService;
import com.bit.scmu_taotao.service.storage.ObjectStorageService;
import com.bit.scmu_taotao.service.upload.ImageUploadService;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.util.storage.ObjectKeyParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final ImageUploadService imageUploadService;
    private final ObjectStorageService objectStorageService;
    private final S3StorageProperties s3StorageProperties;
    private final TGoodsImageService tGoodsImageService;

    @PostMapping("/upload/images")
    public Result uploadImages(@RequestParam("file") MultipartFile[] files) {

        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            return Result.fail(401, "未登录或登录已过期");
        }

        java.util.List<String> imgUrls = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            UploadImageResponse response = imageUploadService.uploadImage(file, userId);
            imgUrls.add(response.getImageUrl());
        }
        java.util.Map<String, java.util.List<String>> resultMap = new java.util.HashMap<>();
        resultMap.put("imgUrls", imgUrls);
        return Result.ok("上传成功", resultMap);
    }

    @DeleteMapping("/delete/images")
    public Result deleteImages(@RequestBody java.util.Map<String, java.util.List<String>> body) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            return Result.fail(401, "未登录或登录已过期");
        }

        java.util.List<String> imageUrls = body == null ? null : body.get("imageUrls");
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Result.fail(400, "imageUrls 不能为空");
        }

        String userPrefix = "user/" + userId + "/";

        java.util.List<String> deleted = new java.util.ArrayList<>();
        java.util.Map<String, String> failed = new java.util.HashMap<>();

        // 去重，避免重复删同一张
        for (String imageUrl : new java.util.LinkedHashSet<>(imageUrls)) {
            if (imageUrl == null || imageUrl.isBlank()) {
                failed.put(String.valueOf(imageUrl), "图片地址为空");
                continue;
            }

            String objectKey = ObjectKeyParser.extractObjectKey(imageUrl, s3StorageProperties.getBucket());
            if (objectKey == null || objectKey.isBlank()) {
                failed.put(imageUrl, "图片地址无效");
                continue;
            }

            if (!objectKey.startsWith(userPrefix)) {
                failed.put(imageUrl, "无权限删除该图片");
                continue;
            }

            long usedCount = tGoodsImageService.count(
                    new LambdaQueryWrapper<TGoodsImage>()
                            .eq(TGoodsImage::getImageUrl, imageUrl)
            );
            if (usedCount > 0) {
                failed.put(imageUrl, "图片已被商品使用，不能删除");
                continue;
            }

            try {
                objectStorageService.deleteObject(objectKey);
                deleted.add(imageUrl);
            } catch (Exception e) {
                failed.put(imageUrl, "对象存储删除失败");
            }
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("deleted", deleted);
        data.put("failed", failed);
        data.put("total", imageUrls.size());

        // 全部失败返回 fail；否则返回 ok + 明细
        if (deleted.isEmpty()) {
            return Result.fail(400, "删除失败");
        }
        return Result.ok("删除完成", data);
    }
}