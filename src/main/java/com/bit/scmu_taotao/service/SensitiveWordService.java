package com.bit.scmu_taotao.service;


import com.bit.scmu_taotao.exception.SensitiveWordException;

public interface SensitiveWordService {
    /**
     * 验证商品信息中的敏感词
     * @param goodsName 商品名称
     * @param desc 商品描述
     * @param remark 商品备注
     * @param purpose 商品用途
     * @param exchangeAddr 交换地址
     * @throws SensitiveWordException 如果检测到敏感词
     */
    void validateGoods(String goodsName, String desc, String remark,
                       String purpose, String exchangeAddr);
}
