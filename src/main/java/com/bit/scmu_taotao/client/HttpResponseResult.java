package com.bit.scmu_taotao.client;

import lombok.Getter;
@Getter
public class HttpResponseResult {

    private final Integer code;
    private final String content;

    public HttpResponseResult(Integer code, String content) {
        this.code = code;
        this.content = content;
    }

}
