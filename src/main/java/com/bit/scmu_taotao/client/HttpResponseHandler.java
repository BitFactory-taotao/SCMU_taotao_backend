package com.bit.scmu_taotao.client;

import org.apache.http.HttpResponse;
import java.io.IOException;

public interface HttpResponseHandler {
    HttpResponseResult parse(HttpResponse response) throws IOException;
}
