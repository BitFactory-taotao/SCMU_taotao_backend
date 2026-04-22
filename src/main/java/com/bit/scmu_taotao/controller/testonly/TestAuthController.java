package com.bit.scmu_taotao.controller.testonly;

import com.bit.scmu_taotao.config.TestOnlyAuthProperties;
import com.bit.scmu_taotao.dto.testonly.TestTokenRequest;
import com.bit.scmu_taotao.dto.testonly.TestTokenResponse;
import com.bit.scmu_taotao.service.testonly.TestTokenService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-only/auth")
@Profile("test")
@ConditionalOnProperty(prefix = "test-only.auth", name = "enabled", havingValue = "true")
public class TestAuthController {

    private final TestTokenService testTokenService;
    private final TestOnlyAuthProperties properties;

    public TestAuthController(TestTokenService testTokenService, TestOnlyAuthProperties properties) {
        this.testTokenService = testTokenService;
        this.properties = properties;
    }

    @PostMapping("/token")
    public Result issueToken(
            @RequestHeader(value = "X-Test-Secret", required = false) String testSecret,
            @Valid @RequestBody TestTokenRequest request) {

        if (!StringUtils.hasText(testSecret) || !testSecret.equals(properties.getSecret())) {
            return Result.fail(403, "forbidden: invalid test secret");
        }

        String token = testTokenService.issueToken(request.getUserId());
        return Result.ok(new TestTokenResponse(request.getUserId(), token, "Bearer"));
    }
}

