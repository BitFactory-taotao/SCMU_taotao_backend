//package com.bit.scmu_taotao.integration;
//
//import com.bit.scmu_taotao.dto.upload.UploadImageResponse;
//import com.bit.scmu_taotao.service.storage.ObjectStorageService;
//import com.bit.scmu_taotao.service.upload.ImageUploadService;
//import org.junit.jupiter.api.Assumptions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class ImageUploadRustfsIntegrationTest {
//
//    private static final Path LOCAL_IMAGE = Path.of("D:\\Administrator\\Pictures\\Saved Pictures\\1.png");
//
//    @Autowired
//    private ImageUploadService imageUploadService;
//
//    @Autowired
//    private ObjectStorageService objectStorageService;
//
//    @Test
//    @DisplayName("集成测试：通过S3协议上传图片到RustFS")
//    void uploadImageToRustfsByS3() throws Exception {
//        Assumptions.assumeTrue(Files.exists(LOCAL_IMAGE), "测试图片不存在: " + LOCAL_IMAGE);
//
//        byte[] bytes = Files.readAllBytes(LOCAL_IMAGE);
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "1.png",
//                "image/png",
//                bytes
//        );
//
//        UploadImageResponse response = null;
//        try {
//            // goodsId 传 null，避免触发 t_goods_image 落库依赖
//            response = imageUploadService.uploadImage(file, "202421091019", null);
//
//            assertNotNull(response);
//            assertNotNull(response.getObjectKey());
//            assertFalse(response.getObjectKey().isBlank());
//            assertNotNull(response.getImageUrl());
//            assertFalse(response.getImageUrl().isBlank());
//            assertEquals("image/png", response.getContentType());
//            assertEquals("1.png", response.getOriginalFilename());
//            assertEquals((long) bytes.length, response.getSize());
//        } finally {
//            // 清理测试数据，避免桶里堆积垃圾文件
//            // if (response != null && response.getObjectKey() != null && !response.getObjectKey().isBlank()) {
//            //     objectStorageService.deleteObject(response.getObjectKey());
//            // }
//        }
//    }
//}