package com.bit.scmu_taotao.service.impl;

import com.bit.scmu_taotao.exception.SensitiveWordException;
import com.bit.scmu_taotao.service.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Value("${goods.sensitive-words:}")
    private String sensitiveWordList;

    private Set<String> wordSet;

    /**
     * 初始化敏感词库，从配置文件加载敏感词
     */
    @PostConstruct
    public void init() {
        wordSet = new HashSet<>();
        if (StringUtils.hasText(sensitiveWordList)) {
            String[] words = sensitiveWordList.split(",");
            for (String word : words) {
                String trimmed = word.trim();
                if (StringUtils.hasText(trimmed)) {
                    wordSet.add(trimmed);
                }
            }
            log.info("敏感词库初始化成功，共加载 {} 个敏感词", wordSet.size());
        } else {
            log.warn("敏感词库未配置，无需检测");
        }
    }

    /**
     * 验证商品信息中的敏感词
     * @param goodsName 商品名称
     * @param desc 商品描述
     * @param remark 商品备注
     * @param purpose 商品用途
     * @param exchangeAddr 交换地址
     * @throws SensitiveWordException 如果检测到敏感词
     */
    @Override
    public void validateGoods(String goodsName, String desc, String remark,
                              String purpose, String exchangeAddr) {
        if (wordSet == null || wordSet.isEmpty()) {
            // 敏感词库为空，跳过检测
            return;
        }

        // 检测商品名称
        String detectedWord = detectSensitiveWord(goodsName);
        if (detectedWord != null) {
            log.warn("商品名称包含敏感词: {}", detectedWord);
            throw new SensitiveWordException(
                    String.format("商品名称包含不合规内容（%s），请修改后重新发布", detectedWord),
                    detectedWord
            );
        }

        // 检测商品描述
        detectedWord = detectSensitiveWord(desc);
        if (detectedWord != null) {
            log.warn("商品描述包含敏感词: {}", detectedWord);
            throw new SensitiveWordException(
                    String.format("商品描述包含不合规内容（%s），请修改后重新发布", detectedWord),
                    detectedWord
            );
        }

        // 检测商品备注
        detectedWord = detectSensitiveWord(remark);
        if (detectedWord != null) {
            log.warn("商品备注包含敏感词: {}", detectedWord);
            throw new SensitiveWordException(
                    String.format("商品备注包含不合规内容（%s），请修改后重新发布", detectedWord),
                    detectedWord
            );
        }

        // 检测商品用途
        detectedWord = detectSensitiveWord(purpose);
        if (detectedWord != null) {
            log.warn("商品用途包含敏感词: {}", detectedWord);
            throw new SensitiveWordException(
                    String.format("商品用途包含不合规内容（%s），请修改后重新发布", detectedWord),
                    detectedWord
            );
        }

        // 检测交换地址
        detectedWord = detectSensitiveWord(exchangeAddr);
        if (detectedWord != null) {
            log.warn("交换地址包含敏感词: {}", detectedWord);
            throw new SensitiveWordException(
                    String.format("交换地址包含不合规内容（%s），请修改后重新发布", detectedWord),
                    detectedWord
            );
        }
    }

    /**
     * 判断文本是否包含敏感词，返回第一个检测到的敏感词
     * @param text 待检测文本
     * @return 检测到的敏感词，若无则返回 null
     */
    private String detectSensitiveWord(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        String lowerText = text.toLowerCase();
        for (String word : wordSet) {
            String lowerWord = word.toLowerCase();
            if (lowerText.contains(lowerWord)) {
                return word;
            }
        }
        return null;
    }
}
