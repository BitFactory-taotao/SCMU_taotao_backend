package com.bit.scmu_taotao.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "storage.s3")
public class S3StorageProperties {
    private String endpoint;
    private String region = "us-east-1";
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true;
    private DataSize maxFileSize = DataSize.ofMegabytes(5);
    private List<String> allowedTypes = new ArrayList<>();
}