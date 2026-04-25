package com.bit.scmu_taotao.util;

import cn.hutool.core.util.RandomUtil;
import com.bit.scmu_taotao.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 工具类
 * 用于生成、验证和管理用户登录 Token
 */
@Component
public class TokenUtil {

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Token 过期时间：2 小时
    private static final long TOKEN_EXPIRE_HOURS = 2;

    // Token 前缀
    private static final String TOKEN_PREFIX = "token_";

    /**
     * 生成 Token
     * @param userId 用户 ID
     * @return Token 字符串
     */
    public String generateToken(String userId) {
        // 生成随机 Token
        String token = TOKEN_PREFIX + System.currentTimeMillis() + RandomUtil.randomNumbers(6);

        // 将 Token 存储到 Redis，同时建立 userId 和 token 的映射关系
        redisService.setWithExpire(token, userId, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        redisService.setWithExpire(TOKEN_PREFIX + userId, token, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

        return token;
    }

    /**
     * 管理员登录生成token
     * @param adminId 管理员 ID
     * @return Token 字符串
     */
    public String generateAdminToken(String adminId) {
        String token = "ADMIN_" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);

        // 存入 Redis 的值加上 "ADMIN:" 前缀
        String storedValue = "ADMIN:" + adminId;

        redisService.setWithExpire(token, storedValue, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

        // 管理员的反向映射 Key 也加个前缀，防止跟学生 ID 冲突
        redisService.setWithExpire("ADMIN_TOKEN_KEY:" + adminId, token, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

        return token;
    }

    /**
     * 验证 Token 是否有效
     * @param token Token 字符串
     * @return 如果有效返回 userId，否则返回 null
     */
    public String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        Object userId = redisService.get(token);
        if (userId != null) {
            // 刷新过期时间
            redisService.setWithExpire(token, userId, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
            return userId.toString();
        }

        return null;
    }

    /**
     * 使 Token 失效（登出时使用）
     * @param token Token 字符串
     */
    public void invalidateToken(String token) {
        if (token != null && !token.isEmpty()) {
            Object userIdObj = redisService.get(token);
            if (userIdObj != null) {
                String userId = userIdObj.toString();
                // 删除 token 和 userId 对应的 token 记录
                redisTemplate.delete(token);
                redisTemplate.delete(TOKEN_PREFIX + userId);
            } else {
                redisTemplate.delete(token);
            }
        }
    }

    /**
     * 获取用户当前有效的 Token
     * @param userId 用户 ID
     * @return Token 字符串，不存在则返回 null
     */
    public String getUserToken(String userId) {
        Object token = redisService.get(TOKEN_PREFIX + userId);
        return token != null ? token.toString() : null;
    }
}
