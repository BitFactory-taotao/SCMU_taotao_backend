package com.bit.scmu_taotao.util.common;

import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class KeyDescription {

    public static final String DEFAULT_AVATAR = "http://localhost:8080/api/v1/campus-taotao/static/images/default-avatar.png";
    public static final String SSFW = "ssfw_";

    public static final String LAB = "lab_";

    public static final String TICKET = "ticket_";
    public static final String FAKEIP = "fakeip_";
    public static final long TRADE_INTENT_EXPIRE_HOURS = 48L;

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String TRADE_INTENT_PREFIX = "trade:req:";
    public static final int GOODS_STATUS_ONLINE = 0;
    public static final int GOODS_STATUS_SOLD = 1;

    // Redis 缓存配置
    public static final String CACHE_KEY_OVERVIEW = "admin:dashboard:overview";
    public static final long CACHE_TTL_SECONDS = 10;

    // Redis 历史快照键前缀
    public static final String TREND_KEY_PREFIX = "admin:dashboard:trend:";
    public static final long TREND_TTL_DAYS = 8;
}
