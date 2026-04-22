package com.bit.scmu_taotao.dto.testonly;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestTokenResponse {

    private String userId;
    private String token;
    private String tokenType;
}

