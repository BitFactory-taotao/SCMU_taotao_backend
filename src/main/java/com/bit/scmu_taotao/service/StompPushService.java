package com.bit.scmu_taotao.service;

public interface StompPushService {

    void pushToUserQueue(String userId, String destination, Object payload);
}

