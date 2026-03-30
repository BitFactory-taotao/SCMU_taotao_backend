package com.bit.scmu_taotao.service.impl.upload;

import com.bit.scmu_taotao.config.storage.S3StorageProperties;
import com.bit.scmu_taotao.dto.upload.UploadImageResponse;
import com.bit.scmu_taotao.entity.TGoodsImage;
import com.bit.scmu_taotao.exception.StorageException;
import com.bit.scmu_taotao.service.TGoodsImageService;
import com.bit.scmu_taotao.service.storage.ObjectStorageService;
import com.bit.scmu_taotao.service.upload.ImageUploadService;
import com.bit.scmu_taotao.util.storage.ObjectKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {

    private final ObjectStorageService objectStorageService;
    private final TGoodsImageService goodsImageService;
    private final S3StorageProperties s3Properties;

    @Override
    public UploadImageResponse uploadImage(MultipartFile file, String userId, Long goodsId) {
        validate(file);

        String objectKey = ObjectKeyGenerator.forUserImage(userId, file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        try (InputStream inputStream = file.getInputStream()) {
            objectStorageService.putObject(objectKey, inputStream, file.getSize(), contentType);
            String imageUrl = objectStorageService.getObjectUrl(objectKey);

            if (goodsId != null) {
                TGoodsImage goodsImage = new TGoodsImage();
                goodsImage.setGoodsId(goodsId);
                goodsImage.setImageUrl(imageUrl);
                goodsImage.setSort(0);
                goodsImage.setCreateTime(LocalDateTime.now());
                goodsImageService.save(goodsImage);
            }

            log.info("image uploaded, userId={}, goodsId={}, key={}", userId, goodsId, objectKey);
            return new UploadImageResponse(objectKey, imageUrl, file.getSize(), contentType, file.getOriginalFilename());
        } catch (Exception e) {
            log.error("image upload failed, userId={}, goodsId={}", userId, goodsId, e);
            throw new StorageException("图片上传失败");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("文件不能为空");
        }

        long maxBytes = s3Properties.getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new StorageException("文件大小超过限制: " + s3Properties.getMaxFileSize());
        }

        String originalFilename = file.getOriginalFilename();
        String ext = extractExt(originalFilename);

        Set<String> allowed = s3Properties.getAllowedTypes().stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (!allowed.isEmpty() && !allowed.contains(ext)) {
            throw new StorageException("文件类型不支持: " + ext);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new StorageException("仅允许上传图片文件");
        }
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}