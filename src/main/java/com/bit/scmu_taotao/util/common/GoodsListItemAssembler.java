package com.bit.scmu_taotao.util.common;

import java.util.HashMap;
import java.util.Map;

public final class GoodsListItemAssembler {

    private GoodsListItemAssembler() {
    }

    public static Map<String, Object> buildGoodsRow(
            Object id, String name, String remark, Object price,
            String imgUrl, Object publishTime, String publisherName, String publisherId) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("remark", remark);
        item.put("price", price);
        item.put("imgUrl", imgUrl);
        item.put("publishTime", publishTime);
        item.put("publisherName", publisherName);
        item.put("publisherId", publisherId);
        return item;
    }
}
