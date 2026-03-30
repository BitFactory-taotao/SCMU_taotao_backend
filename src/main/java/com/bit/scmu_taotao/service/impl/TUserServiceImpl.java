package com.bit.scmu_taotao.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.client.HttpResponseHandler;
import com.bit.scmu_taotao.client.HttpResponseHandlerImpl;
import com.bit.scmu_taotao.client.HttpResponseResult;
import com.bit.scmu_taotao.client.thread.WebVpnLoginThread;
import com.bit.scmu_taotao.dto.goods.PublisherDTO;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.TUserMapper;
import com.bit.scmu_taotao.service.RedisService;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.common.KeyDescription;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
* @author 35314
* @description 针对表【t_user(用户基础信息表)】的数据库操作 Service 实现
* @createDate 2026-03-12 18:35:11
*/
@Slf4j
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser>
    implements TUserService{

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenUtil tokenUtil;

    // WebVPN 地址（从配置文件读取或默认值）
    private static final String WEBVPN_URL = "https://webvpn.scuec.edu.cn/";

    private final HttpResponseHandler httpResponseHandler = new HttpResponseHandlerImpl();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result login(String userId, String password) {
        try {
            log.info("用户登录：userId={}", userId);

            // 1. 通过 WebVPN 进行身份验证
            CookieStore cookieStore = new BasicCookieStore();
            WebVpnLoginThread loginThread = new WebVpnLoginThread(
                userId, password, cookieStore, WEBVPN_URL, redisService
            );

            // 执行同步登录
            Result vpnLoginResult = loginThread.syncLogin();
            if (vpnLoginResult.getCode() != 200) {
                return Result.fail("登录失败");
            }

            // 2. 登录成功后，获取真实用户名
            String realUserName = (String) vpnLoginResult.getData();
            log.info("webvpn 登录成功，真实用户名：{}", realUserName);
            // 3. 查询或创建用户记录
            TUser user = this.getById(userId);
            if (user == null) {
                // 首次登录，创建新用户
                user = new TUser();
                user.setUserId(userId);
                user.setUserName(realUserName != null ? realUserName : userId);
                user.setCreditScore(100); // 初始信誉分 100
                user.setCreditStar(new BigDecimal("5.0")); // 初始信誉星级 5.0
                user.setCreateTime(java.time.LocalDateTime.now()); // 设置创建时间
                user.setUpdateTime(java.time.LocalDateTime.now()); // 设置更新时间
                user.setIsDelete(0); // 未删除
                this.save(user);
                log.info("新用户注册：userId={}, userName={}", userId, user.getUserName());
            } else {
                // 更新用户名（如果发生变化）
                if (realUserName != null && !realUserName.equals(user.getUserName())) {
                    user.setUserName(realUserName);
                    user.setUpdateTime(java.time.LocalDateTime.now()); // 更新更新时间
                    this.updateById(user);
                }
            }

            // 4. 序列化 Cookie 并存储到 Redis
            String cookieKey = KeyDescription.SSFW + userId;
            try {
                byte[] serializedCookie = CookieSerialization.serialize(cookieStore);
                redisService.setWithExpire(cookieKey, serializedCookie, 2, java.util.concurrent.TimeUnit.HOURS);
                log.info("Cookie 已保存：key={}", cookieKey);
            } catch (IOException e) {
                log.error("Cookie 序列化失败：{}", e.getMessage(), e);
            }

            // 5. 生成 Token
            String token = tokenUtil.generateToken(userId);

            // 6. 构建符合接口文档的返回数据
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("name", user.getUserName());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("userInfo", userInfo);

            log.info("登录成功：userId={}, token={}", userId, token);
            return Result.ok("登录成功", responseData);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            // 判断是否是登录异常
            if (errorMsg != null) {
                if (errorMsg.contains("IP_BLOCK") || errorMsg.contains("ip 被冻结")) {
                    log.error("登录失败：IP 被冻结");
                    return Result.fail("IP 被冻结，请稍后重试");
                } else if (errorMsg.contains("LOGIN_FAILURE") || errorMsg.contains("用户名或密码错误")) {
                    log.error("登录失败：用户名或密码错误");
                    return Result.fail("用户名或密码错误");
                }
            }
            log.error("登录异常：{}", e.getMessage(), e);
            return Result.fail("登录失败，请稍后重试");
        }
    }

    @Override
    public Result logout(String token) {
        try {
            // 验证 Token
            String userId = tokenUtil.validateToken(token);
            if (userId == null) {
                return Result.fail("Token 无效或已过期");
            }

            // 使 Token 失效
            tokenUtil.invalidateToken(token);

            // 删除 Redis 中的 Cookie
            String cookieKey = KeyDescription.SSFW + userId;
            redisService.isExist(cookieKey); // 检查是否存在

            log.info("用户登出：userId={}", userId);
            return Result.ok();
        } catch (Exception e) {
            log.error("登出失败：{}", e.getMessage(), e);
            return Result.fail("登出失败，请稍后重试");
        }
    }

    @Override
    public Result getUserInfoByToken(String token) {
        try {
            // 验证 Token
            String userId = tokenUtil.validateToken(token);
            if (userId == null) {
                return Result.fail("Token 无效或已过期");
            }

            // 查询用户信息
            TUser user = this.getById(userId);
            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 返回用户信息（排除敏感字段）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("creditScore", user.getCreditScore());
            userInfo.put("creditStar", user.getCreditStar());

            return Result.ok(userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败：{}", e.getMessage(), e);
            return Result.fail("获取用户信息失败，请稍后重试");
        }
    }
    @Override
    public PublisherDTO getPublisherInfo(String userId) {
        // 查询用户信息
        TUser user = this.getById(userId);
        if (user == null) {
            return null;
        }

        // 构建发布者信息DTO
        PublisherDTO publisher = new PublisherDTO();
        publisher.setId(user.getUserId());
        publisher.setName(user.getUserName());
        publisher.setCreditScore(user.getCreditScore());
        // 将 BigDecimal 转换为 Double
        if (user.getCreditStar() != null) {
            publisher.setCreditStar(user.getCreditStar().doubleValue());
        }

        return publisher;
    }
}



