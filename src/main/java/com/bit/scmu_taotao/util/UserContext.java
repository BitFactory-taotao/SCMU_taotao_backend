package com.bit.scmu_taotao.util;

import lombok.Data;
import org.apache.http.client.CookieStore;

public class UserContext {
    // ThreadLocal用于存储当前登录用户的 userId
    private final static ThreadLocal<String> USER_ID_CONTEXT = new ThreadLocal<>();

    // ThreadLocal用于存储用户信息（原有逻辑保留，但增加 userId 的快捷存取）
    @Data
    static class UserInfo {
        private String userId;
        private CookieStore cookieStore;
        private String username;
        private String password;


        public UserInfo(CookieStore cookieStore,String username, String password, String userId) {
            this.cookieStore = cookieStore;
            this.username = username;
            this.password = password;
            this.userId = userId;
        }
    }

    private final static ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前登录用户 ID
     */
    public static void setUserId(String userId) {
        USER_ID_CONTEXT.set(userId);
    }

    /**
     * 获取当前登录用户 ID
     */
    public static String getUserId() {
        return USER_ID_CONTEXT.get();
    }

    // ThreadLocal.set (保留并增强)
    public static void set(CookieStore cookieStore, String username, String password, String userId) {
        UserInfo userInfo = new UserInfo(cookieStore,username, password, userId);
        CONTEXT.set(userInfo);
        USER_ID_CONTEXT.set(userId);
    }

    // 兼容旧方法的 set
    public static void set(CookieStore cookieStore, String username, String password) {
        set(cookieStore, username, password, null);
    }

    public static CookieStore getCookieStore() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.getCookieStore() : null;
    }

    public static String getUsername() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.getUsername() : null;
    }

    public static String getPassword() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.getPassword() : null;
    }

    // ThreadLocal.remove
    public static void remove() {
        CONTEXT.remove();
        USER_ID_CONTEXT.remove();
    }
}
