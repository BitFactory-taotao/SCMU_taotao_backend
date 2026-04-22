package com.bit.scmu_taotao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "test-only.auth")
public class TestOnlyAuthProperties {

    private boolean enabled = false;
    private String secret;
}

