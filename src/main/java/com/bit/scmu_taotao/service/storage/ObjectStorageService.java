package com.bit.scmu_taotao.service.storage;

import java.io.InputStream;

public interface ObjectStorageService {

    String putObject(String objectKey, InputStream inputStream, long contentLength, String contentType);

    void deleteObject(String objectKey);

    String getObjectUrl(String objectKey);
}