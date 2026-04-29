package com.bit.scmu_taotao.service.upload;

import com.bit.scmu_taotao.dto.upload.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {

    UploadImageResponse uploadImage(MultipartFile file, String userId);
}