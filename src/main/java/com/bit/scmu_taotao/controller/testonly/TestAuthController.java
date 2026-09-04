package com.bit.scmu_taotao.controller.testonly;

import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 联调专用：为任意用户签发临时 token。
 * 仅 test profile 下加载；生产 profile 不激活，天然关闭。
 */
@RestController
@RequestMapping("/test-only/auth")
@Profile("test")
public class TestAuthController {

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 签发 token：POST /test-only/auth/token?userId=xxx
     */
    @PostMapping("/token")
    public Result issueToken(@RequestParam("userId") String userId) {
        String token = tokenUtil.generateToken(userId);
        return Result.ok(Map.of("userId", userId, "token", token, "tokenType", "Bearer"));
    }
}
