package com.bit.scmu_taotao.service.testonly.impl;

import com.bit.scmu_taotao.service.testonly.TestTokenService;
import com.bit.scmu_taotao.util.TokenUtil;
import org.springframework.stereotype.Service;

@Service
public class TestTokenServiceImpl implements TestTokenService {

    private final TokenUtil tokenUtil;

    public TestTokenServiceImpl(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    @Override
    public String issueToken(String userId) {
        return tokenUtil.generateToken(userId);
    }
}

