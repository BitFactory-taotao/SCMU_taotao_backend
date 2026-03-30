package com.bit.scmu_taotao.service.storage;

import com.bit.scmu_taotao.config.storage.S3StorageProperties;
import com.bit.scmu_taotao.exception.StorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ObjectStorageServiceImpl implements ObjectStorageService {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    @Override
    public String putObject(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            return objectKey;
        } catch (Exception e) {
            log.error("S3 putObject failed, key={}", objectKey, e);
            throw new StorageException("上传到对象存储失败");
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            log.error("S3 deleteObject failed, key={}", objectKey, e);
            throw new StorageException("删除对象失败");
        }
    }

    @Override
    public String getObjectUrl(String objectKey) {
        try {
            return s3Client.utilities()
                    .getUrl(GetUrlRequest.builder()
                            .bucket(properties.getBucket())
                            .key(objectKey)
                            .build())
                    .toExternalForm();
        } catch (Exception e) {
            log.error("S3 getObjectUrl failed, key={}", objectKey, e);
            throw new StorageException("生成对象访问地址失败");
        }
    }
}