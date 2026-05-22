package com.bit.scmu_taotao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户举报请求参数
 */
@Data
public class UserReportRequest {

    /**
     * 举报分类
     */
    @NotBlank(message = "举报分类不能为空")
    @Pattern(regexp = "^(LOW_CREDIT|GOODS_VIOLATION|LANG_VIOLATION|OTHER)$", message = "举报分类不合法")
    private String category;

    /**
     * 举报内容
     */
    @NotBlank(message = "举报内容不能为空")
    @Size(max = 800, message = "举报内容不能超过800字")
    private String content;

    /**
     * 证据图片地址列表
     */
    @Size(max = 4, message = "证据图片最多4张")
    private List<String> imgUrls;
}

