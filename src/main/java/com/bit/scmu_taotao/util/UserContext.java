package com.bit.scmu_taotao.util;

public class UserContext {
    // ThreadLocal用于存储当前登录用户的 userId
    private final static ThreadLocal<String> USER_ID_CONTEXT = new ThreadLocal<>();

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

    // ThreadLocal.remove
    public static void remove() {
        USER_ID_CONTEXT.remove();
    }
}
