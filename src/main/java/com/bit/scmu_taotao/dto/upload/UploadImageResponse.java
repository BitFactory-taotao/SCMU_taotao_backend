package com.bit.scmu_taotao.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadImageResponse {
    private String objectKey;
    private String imageUrl;
    private Long size;
    private String contentType;
    private String originalFilename;
}