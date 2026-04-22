package com.bit.scmu_taotao.dto.testonly;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestTokenRequest {

    @NotBlank(message = "userId cannot be blank")
    private String userId;
}

