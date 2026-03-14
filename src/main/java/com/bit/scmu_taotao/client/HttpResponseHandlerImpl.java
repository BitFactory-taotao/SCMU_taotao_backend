package com.bit.scmu_taotao.client;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpResponseHandlerImpl implements HttpResponseHandler {
    @Override
    public HttpResponseResult parse(HttpResponse response) throws IOException {
        Integer code = response.getStatusLine().getStatusCode();
        String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return new HttpResponseResult(code, content);
    }
}
